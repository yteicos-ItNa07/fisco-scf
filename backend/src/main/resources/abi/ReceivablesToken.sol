pragma solidity ^0.4.25;

/**
 * @title ReceivablesToken - 应收账款通证合约
 * @dev 实现应收账款的签发、拆分、转让、融资等核心功能
 * 适用于 FISCO-BCOS 2.x (Solidity 0.4.25)
 */
contract ReceivablesToken {

    // 通证结构体
    struct Token {
        uint256 tokenId;
        uint256 parentTokenId;
        address issuer;
        address holder;
        uint256 faceValue;
        uint256 remainingValue;
        uint256 maturityDate;
        uint256 annualRate;
        string contractHash;
        uint8 status;  // 0:CREATED, 1:CONFIRMED, 2:FINANCED, 3:PARTIALLY_FINANCED, 4:SETTLED
        uint256 createdAt;
    }

    // 融资记录结构体
    struct FinancingRecord {
        uint256 recordId;
        uint256 tokenId;
        address borrower;
        address financier;
        uint256 financingAmount;
        uint256 annualRate;
        uint256 discountAmount;
        uint256 actualAmount;
        uint8 status;  // 0:PENDING, 1:APPROVED, 2:REJECTED, 3:DISBURSED
        uint256 createdAt;
    }

    // 状态变量
    mapping(uint256 => Token) public tokens;
    mapping(uint256 => FinancingRecord) public financingRecords;
    uint256 public tokenCount;
    uint256 public financingCount;
    address public owner;

    // 事件定义
    event TokenIssued(uint256 indexed tokenId, address indexed issuer, address indexed holder, uint256 faceValue);
    event TokenConfirmed(uint256 indexed tokenId);
    event TokenTransferred(uint256 indexed tokenId, address indexed from, address indexed to);
    event FinancingApplied(uint256 indexed recordId, uint256 indexed tokenId, address borrower, uint256 amount);
    event FinancingApproved(uint256 indexed recordId, bool approved);
    event FinancingDisbursed(uint256 indexed recordId);
    event TokenSplit(uint256 indexed parentTokenId, uint256 newTokenCount);

    // 构造函数
    constructor() public {
        owner = msg.sender;
        tokenCount = 0;
        financingCount = 0;
    }

    /**
     * @dev 签发应收账款通证
     */
    function issueToken(
        address _holder,
        uint256 _faceValue,
        uint256 _maturityDate,
        uint256 _annualRate,
        string _contractHash
    ) public returns (uint256) {
        require(_holder != address(0), "Invalid holder");
        require(_faceValue > 0, "Invalid face value");
        require(_maturityDate > now, "Invalid maturity date");

        tokenCount++;
        uint256 newTokenId = tokenCount;

        tokens[newTokenId] = Token({
            tokenId: newTokenId,
            parentTokenId: 0,
            issuer: msg.sender,
            holder: _holder,
            faceValue: _faceValue,
            remainingValue: _faceValue,
            maturityDate: _maturityDate,
            annualRate: _annualRate,
            contractHash: _contractHash,
            status: 0,  // CREATED
            createdAt: now
        });

        emit TokenIssued(newTokenId, msg.sender, _holder, _faceValue);
        return newTokenId;
    }

    /**
     * @dev 确权通证
     */
    function confirmToken(uint256 _tokenId) public {
        require(_tokenId > 0 && _tokenId <= tokenCount, "Token not exist");
        Token storage token = tokens[_tokenId];
        require(token.issuer == msg.sender, "Not issuer");
        require(token.status == 0, "Not in CREATED status");

        token.status = 1;  // CONFIRMED
        emit TokenConfirmed(_tokenId);
    }

    /**
     * @dev 拆分通证 - 将一个通证拆分为多个子通证
     * 仅限 owner 或 holder 调用
     */
    function splitToken(
        uint256 _tokenId,
        address[] _recipients,
        uint256[] _amounts
    ) public returns (uint256[]) {
        require(_tokenId > 0 && _tokenId <= tokenCount, "Token not exist");
        require(_recipients.length == _amounts.length, "Length mismatch");
        require(_recipients.length > 0, "Empty split");

        Token storage token = tokens[_tokenId];
        require(token.holder == msg.sender || msg.sender == owner, "Not holder or owner");
        require(token.status == 1 || token.status == 3, "Invalid status");

        // 计算总拆分金额
        uint256 totalAmount = 0;
        for (uint256 i = 0; i < _amounts.length; i++) {
            require(_amounts[i] > 0, "Invalid amount");
            require(_recipients[i] != address(0), "Invalid recipient");
            totalAmount += _amounts[i];
        }
        require(totalAmount <= token.remainingValue, "Exceeds remaining");

        // 创建子通证
        uint256[] memory newTokenIds = new uint256[](_recipients.length);
        for (uint256 j = 0; j < _recipients.length; j++) {
            tokenCount++;
            uint256 newId = tokenCount;
            newTokenIds[j] = newId;

            tokens[newId] = Token({
                tokenId: newId,
                parentTokenId: _tokenId,
                issuer: token.issuer,
                holder: _recipients[j],
                faceValue: _amounts[j],
                remainingValue: _amounts[j],
                maturityDate: token.maturityDate,
                annualRate: token.annualRate,
                contractHash: token.contractHash,
                status: 1,  // CONFIRMED
                createdAt: now
            });

            emit TokenIssued(newId, token.issuer, _recipients[j], _amounts[j]);
        }

        // 更新原通证剩余金额
        token.remainingValue -= totalAmount;

        return newTokenIds;
    }

    /**
     * @dev 转让通证
     * 仅限 owner 或 holder 调用
     */
    function transferToken(uint256 _tokenId, address _to) public {
        require(_tokenId > 0 && _tokenId <= tokenCount, "Token not exist");
        require(_to != address(0), "Invalid recipient");

        Token storage token = tokens[_tokenId];
        require(token.holder == msg.sender || msg.sender == owner, "Not holder or owner");
        require(token.status == 1 || token.status == 3, "Invalid status");

        address from = token.holder;
        token.holder = _to;
        emit TokenTransferred(_tokenId, from, _to);
    }

    /**
     * @dev 申请融资
     * 仅限 owner 或 holder 调用
     */
    function applyFinancing(
        uint256 _tokenId,
        address _financier,
        uint256 _amount,
        uint256 _rate
    ) public returns (uint256) {
        require(_tokenId > 0 && _tokenId <= tokenCount, "Token not exist");
        Token storage token = tokens[_tokenId];
        require(token.holder == msg.sender || msg.sender == owner, "Not holder or owner");
        require(_amount <= token.remainingValue, "Amount exceeds remaining");

        financingCount++;
        uint256 newRecordId = financingCount;

        // 计算折价
        uint256 daysRemaining = (token.maturityDate - now) / 86400;
        uint256 discount = (_amount * _rate * daysRemaining) / (365 * 10000);
        uint256 actual = _amount - discount;

        financingRecords[newRecordId] = FinancingRecord({
            recordId: newRecordId,
            tokenId: _tokenId,
            borrower: msg.sender,
            financier: _financier,
            financingAmount: _amount,
            annualRate: _rate,
            discountAmount: discount,
            actualAmount: actual,
            status: 0,  // PENDING
            createdAt: now
        });

        emit FinancingApplied(newRecordId, _tokenId, msg.sender, _amount);
        return newRecordId;
    }

    /**
     * @dev 审批融资
     * 仅限 owner 或 financier 调用
     */
    function approveFinancing(uint256 _recordId, bool _approved) public {
        require(_recordId > 0 && _recordId <= financingCount, "Record not exist");
        FinancingRecord storage record = financingRecords[_recordId];
        require(record.financier == msg.sender || msg.sender == owner, "Not financier or owner");
        require(record.status == 0, "Not pending");

        if (_approved) {
            record.status = 1;  // APPROVED
        } else {
            record.status = 2;  // REJECTED
        }
        emit FinancingApproved(_recordId, _approved);
    }

    /**
     * @dev 放款
     * 仅限 owner 或 financier 调用
     */
    function disburseFinancing(uint256 _recordId) public {
        require(_recordId > 0 && _recordId <= financingCount, "Record not exist");
        FinancingRecord storage record = financingRecords[_recordId];
        require(record.financier == msg.sender || msg.sender == owner, "Not financier or owner");
        require(record.status == 1, "Not approved");

        Token storage token = tokens[record.tokenId];
        record.status = 3;  // DISBURSED
        token.remainingValue -= record.financingAmount;

        if (token.remainingValue == 0) {
            token.status = 2;  // FINANCED
        } else {
            token.status = 3;  // PARTIALLY_FINANCED
        }

        emit FinancingDisbursed(_recordId);
    }

    // ============ 查询函数 ============

    function getToken(uint256 _tokenId) public view returns (
        uint256 tokenId,
        address issuer,
        address holder,
        uint256 faceValue,
        uint256 remainingValue,
        uint256 maturityDate,
        uint8 status
    ) {
        Token storage t = tokens[_tokenId];
        return (t.tokenId, t.issuer, t.holder, t.faceValue, t.remainingValue, t.maturityDate, t.status);
    }

    function getTokenCount() public view returns (uint256) {
        return tokenCount;
    }

    function getFinancingRecord(uint256 _recordId) public view returns (
        uint256 recordId,
        uint256 tokenId,
        address borrower,
        address financier,
        uint256 financingAmount,
        uint256 actualAmount,
        uint8 status
    ) {
        FinancingRecord storage r = financingRecords[_recordId];
        return (r.recordId, r.tokenId, r.borrower, r.financier, r.financingAmount, r.actualAmount, r.status);
    }
}
