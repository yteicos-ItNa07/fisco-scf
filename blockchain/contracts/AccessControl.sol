pragma solidity ^0.4.25;

/**
 * @title AccessControl - 访问控制与企业管理合约
 * @dev 管理企业注册、角色权限、信用评分
 * 适用于 FISCO-BCOS 2.x (Solidity 0.4.25)
 */
contract AccessControl {

    // 企业角色常量
    bytes32 public constant ROLE_CORE_ENTERPRISE = "CORE_ENTERPRISE";
    bytes32 public constant ROLE_SUPPLIER = "SUPPLIER";
    bytes32 public constant ROLE_FINANCIAL_INST = "FINANCIAL_INST";

    // 企业信息结构体
    struct Enterprise {
        address account;
        string name;
        string creditCode;
        bytes32 role;
        uint256 creditScore;
        bool isActive;
        uint256 registeredAt;
    }

    // 状态变量
    mapping(address => Enterprise) public enterprises;
    address[] public enterpriseList;
    address public owner;

    // 事件定义
    event EnterpriseRegistered(address indexed account, string name, bytes32 role);
    event CreditScoreUpdated(address indexed account, uint256 oldScore, uint256 newScore);

    // 构造函数
    constructor() public {
        owner = msg.sender;
    }

    // 修饰器
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner");
        _;
    }

    /**
     * @dev 注册企业
     */
    function registerEnterprise(
        address _account,
        string _name,
        string _creditCode,
        bytes32 _role
    ) public {
        require(_account != address(0), "Invalid account");
        require(!enterprises[_account].isActive, "Already registered");

        uint256 defaultScore = 700;
        if (_role == ROLE_CORE_ENTERPRISE) {
            defaultScore = 850;
        } else if (_role == ROLE_FINANCIAL_INST) {
            defaultScore = 900;
        }

        enterprises[_account] = Enterprise({
            account: _account,
            name: _name,
            creditCode: _creditCode,
            role: _role,
            creditScore: defaultScore,
            isActive: true,
            registeredAt: now
        });

        enterpriseList.push(_account);
        emit EnterpriseRegistered(_account, _name, _role);
    }

    /**
     * @dev 更新信用评分
     */
    function updateCreditScore(address _account, uint256 _newScore) public {
        require(_newScore <= 1000, "Score must be <= 1000");
        require(enterprises[_account].isActive, "Not registered");

        uint256 oldScore = enterprises[_account].creditScore;
        enterprises[_account].creditScore = _newScore;
        emit CreditScoreUpdated(_account, oldScore, _newScore);
    }

    /**
     * @dev 获取企业信息
     */
    function getEnterprise(address _account) public view returns (
        string name,
        string creditCode,
        bytes32 role,
        uint256 creditScore,
        bool isActive
    ) {
        Enterprise storage e = enterprises[_account];
        return (e.name, e.creditCode, e.role, e.creditScore, e.isActive);
    }

    /**
     * @dev 获取信用评分
     */
    function getCreditScore(address _account) public view returns (uint256) {
        return enterprises[_account].creditScore;
    }

    /**
     * @dev 检查角色
     */
    function hasRole(address _account, bytes32 _role) public view returns (bool) {
        return enterprises[_account].isActive && enterprises[_account].role == _role;
    }

    /**
     * @dev 获取企业数量
     */
    function getEnterpriseCount() public view returns (uint256) {
        return enterpriseList.length;
    }
}
