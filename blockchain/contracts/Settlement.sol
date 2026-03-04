pragma solidity ^0.4.25;

/**
 * @title Settlement - 结算合约
 * @dev 管理应收账款的到期结算和融资还款
 * 适用于 FISCO-BCOS 2.x (block.timestamp返回毫秒)
 */
contract Settlement {

    // 结算状态: 0=PENDING, 1=COMPLETED, 2=FAILED
    // 还款状态: isRepaid = true/false

    struct SettlementRecord {
        uint256 settlementId;
        uint256 tokenId;
        address payer;          // 付款方（核心企业）
        address payee;          // 收款方（金融机构或持有方）
        uint256 amount;         // 结算总金额
        uint256 principal;      // 本金
        uint256 interest;       // 利息
        uint8 status;           // 0=PENDING, 1=COMPLETED, 2=FAILED
        string txReference;     // 银行交易参考号
        uint256 createdAt;
        uint256 completedAt;
    }

    struct RepaymentPlan {
        uint256 planId;
        uint256 financingRecordId;
        uint256 tokenId;
        address borrower;
        address financier;
        uint256 totalAmount;    // 总还款金额 (本金+利息)
        uint256 principal;
        uint256 interest;
        uint256 dueDate;        // 到期日 (毫秒)
        bool isRepaid;
        uint256 repaidAt;
    }

    mapping(uint256 => SettlementRecord) public settlements;
    mapping(uint256 => RepaymentPlan) public repaymentPlans;
    mapping(uint256 => uint256[]) public tokenSettlements;  // tokenId => settlementIds

    uint256 public settlementCount;
    uint256 public repaymentPlanCount;
    address public owner;

    event SettlementCreated(uint256 indexed settlementId, uint256 indexed tokenId, address payer, address payee, uint256 amount);
    event SettlementCompleted(uint256 indexed settlementId, string txReference);
    event SettlementFailed(uint256 indexed settlementId, string reason);
    event RepaymentPlanCreated(uint256 indexed planId, uint256 financingRecordId, uint256 totalAmount, uint256 dueDate);
    event RepaymentCompleted(uint256 indexed planId, uint256 amount);

    constructor() public {
        owner = msg.sender;
        settlementCount = 0;
        repaymentPlanCount = 0;
    }

    /**
     * @dev 创建结算记录
     */
    function createSettlement(
        uint256 _tokenId,
        address _payer,
        address _payee,
        uint256 _principal,
        uint256 _interest
    ) public returns (uint256) {
        require(_payer != address(0), "Invalid payer");
        require(_payee != address(0), "Invalid payee");
        require(_principal > 0, "Principal must be positive");

        settlementCount++;
        uint256 newId = settlementCount;
        uint256 totalAmount = _principal + _interest;

        settlements[newId] = SettlementRecord({
            settlementId: newId,
            tokenId: _tokenId,
            payer: _payer,
            payee: _payee,
            amount: totalAmount,
            principal: _principal,
            interest: _interest,
            status: 0,
            txReference: "",
            createdAt: now,
            completedAt: 0
        });

        tokenSettlements[_tokenId].push(newId);

        emit SettlementCreated(newId, _tokenId, _payer, _payee, totalAmount);
        return newId;
    }

    /**
     * @dev 确认结算完成
     */
    function confirmSettlement(uint256 _settlementId, string _txReference) public {
        require(_settlementId > 0 && _settlementId <= settlementCount, "Settlement not found");

        SettlementRecord storage s = settlements[_settlementId];
        require(s.status == 0, "Settlement not pending");
        require(msg.sender == s.payer || msg.sender == owner, "Not authorized");

        s.status = 1;
        s.txReference = _txReference;
        s.completedAt = now;

        emit SettlementCompleted(_settlementId, _txReference);
    }

    /**
     * @dev 标记结算失败
     */
    function failSettlement(uint256 _settlementId, string _reason) public {
        require(msg.sender == owner, "Only owner");
        require(_settlementId > 0 && _settlementId <= settlementCount, "Settlement not found");

        SettlementRecord storage s = settlements[_settlementId];
        require(s.status == 0, "Settlement not pending");

        s.status = 2;
        emit SettlementFailed(_settlementId, _reason);
    }

    /**
     * @dev 创建还款计划
     */
    function createRepaymentPlan(
        uint256 _financingRecordId,
        uint256 _tokenId,
        address _borrower,
        address _financier,
        uint256 _principal,
        uint256 _interest,
        uint256 _dueDate
    ) public returns (uint256) {
        require(_borrower != address(0), "Invalid borrower");
        require(_financier != address(0), "Invalid financier");
        require(_dueDate > now, "Due date must be in future");

        repaymentPlanCount++;
        uint256 newId = repaymentPlanCount;

        repaymentPlans[newId] = RepaymentPlan({
            planId: newId,
            financingRecordId: _financingRecordId,
            tokenId: _tokenId,
            borrower: _borrower,
            financier: _financier,
            totalAmount: _principal + _interest,
            principal: _principal,
            interest: _interest,
            dueDate: _dueDate,
            isRepaid: false,
            repaidAt: 0
        });

        emit RepaymentPlanCreated(newId, _financingRecordId, _principal + _interest, _dueDate);
        return newId;
    }

    /**
     * @dev 确认还款完成
     */
    function confirmRepayment(uint256 _planId) public {
        require(_planId > 0 && _planId <= repaymentPlanCount, "Plan not found");

        RepaymentPlan storage plan = repaymentPlans[_planId];
        require(!plan.isRepaid, "Already repaid");
        require(
            msg.sender == plan.borrower ||
            msg.sender == plan.financier ||
            msg.sender == owner,
            "Not authorized"
        );

        plan.isRepaid = true;
        plan.repaidAt = now;

        emit RepaymentCompleted(_planId, plan.totalAmount);
    }

    // ============ 查询函数 ============

    function getSettlement(uint256 _id) public view returns (
        uint256 settlementId, uint256 tokenId, address payer, address payee,
        uint256 amount, uint8 status, uint256 completedAt
    ) {
        SettlementRecord storage s = settlements[_id];
        return (s.settlementId, s.tokenId, s.payer, s.payee, s.amount, s.status, s.completedAt);
    }

    function getRepaymentPlan(uint256 _id) public view returns (
        uint256 planId, uint256 tokenId, address borrower, address financier,
        uint256 totalAmount, uint256 dueDate, bool isRepaid
    ) {
        RepaymentPlan storage p = repaymentPlans[_id];
        return (p.planId, p.tokenId, p.borrower, p.financier, p.totalAmount, p.dueDate, p.isRepaid);
    }

    function getTokenSettlements(uint256 _tokenId) public view returns (uint256[]) {
        return tokenSettlements[_tokenId];
    }

    /**
     * @dev 检查是否逾期 (FISCO-BCOS 2.x: now返回毫秒)
     */
    function isOverdue(uint256 _planId) public view returns (bool) {
        RepaymentPlan storage plan = repaymentPlans[_planId];
        return !plan.isRepaid && now > plan.dueDate;
    }

    /**
     * @dev 计算逾期天数 (FISCO-BCOS 2.x: now返回毫秒, 86400000ms = 1天)
     */
    function getOverdueDays(uint256 _planId) public view returns (uint256) {
        RepaymentPlan storage plan = repaymentPlans[_planId];
        if (plan.isRepaid || now <= plan.dueDate) {
            return 0;
        }
        return (now - plan.dueDate) / 86400000;
    }
}
