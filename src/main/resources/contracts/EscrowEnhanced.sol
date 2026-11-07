// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0 <0.9.0;

import "./Escrow.sol";

/// Route A bonus contract. A separate deployment preserves all original coursework receipts.
contract EscrowEnhanced {
    // Keep the original numeric values; append Shipped rather than renumbering terminal states.
    enum Status { Created, Paid, Completed, Refunded, Disputed, Shipped }
    struct Order {
        address buyer;
        address seller;
        address token;
        uint256 amount;
        uint256 deadline;
        Status status;
        string description;
    }
    struct Vote { bool cast; bool sellerWins; }

    address public immutable arbiter;
    address public immutable secondArbiter;
    uint256 public orderCount;
    mapping(uint256 => Order) private _orders;
    mapping(uint256 => mapping(address => Vote)) private _votes;

    event OrderCreated(uint256 indexed id, address indexed buyer, address indexed seller, address token, uint256 amount, uint256 deadline);
    event OrderPaid(uint256 indexed id);
    event OrderCompleted(uint256 indexed id);
    event OrderRefunded(uint256 indexed id);
    event OrderDisputed(uint256 indexed id, address by);
    event OrderResolved(uint256 indexed id, bool sellerWins);
    event OrderShipped(uint256 indexed id, address seller);
    event ArbitrationVoted(uint256 indexed id, address indexed voter, bool sellerWins);

    modifier exists(uint256 id) {
        require(id < orderCount, "Escrow: order does not exist");
        _;
    }
    modifier onlyBuyer(uint256 id) {
        require(msg.sender == _orders[id].buyer, "Escrow: caller is not the buyer");
        _;
    }
    modifier inStatus(uint256 id, Status state) {
        require(_orders[id].status == state, "Escrow: wrong status");
        _;
    }

    constructor(address first, address second) {
        require(first != address(0) && second != address(0), "Escrow: zero arbiter");
        require(first != second, "Escrow: arbiters must differ");
        arbiter = first;
        secondArbiter = second;
    }

    function version() external pure returns (uint256) { return 2; }

    function getOrder(uint256 id) external view exists(id) returns (Order memory) {
        return _orders[id];
    }

    function getVotes(uint256 id) external view exists(id) returns
        (bool firstVoted, bool firstSellerWins, bool secondVoted, bool secondSellerWins) {
        Vote memory first = _votes[id][arbiter];
        Vote memory second = _votes[id][secondArbiter];
        return (first.cast, first.sellerWins, second.cast, second.sellerWins);
    }

    function createOrder(address seller, address token, uint256 amount, uint256 deadline,
                         string calldata description) external returns (uint256 id) {
        require(seller != address(0) && seller != msg.sender, "Escrow: invalid seller");
        require(token.code.length > 0, "Escrow: token is not a contract");
        require(amount > 0, "Escrow: amount is zero");
        require(deadline > block.timestamp, "Escrow: deadline in the past");
        id = orderCount++;
        _orders[id] = Order(msg.sender, seller, token, amount, deadline, Status.Created, description);
        emit OrderCreated(id, msg.sender, seller, token, amount, deadline);
    }

    function deposit(uint256 id) external exists(id) onlyBuyer(id) inStatus(id, Status.Created) {
        Order storage o = _orders[id];
        require(block.timestamp <= o.deadline, "Escrow: deadline passed");
        o.status = Status.Paid;
        require(IERC20(o.token).transferFrom(msg.sender, address(this), o.amount), "Escrow: transferFrom failed");
        emit OrderPaid(id);
    }

    function markShipped(uint256 id) external exists(id) inStatus(id, Status.Paid) {
        Order storage o = _orders[id];
        require(msg.sender == o.seller, "Escrow: caller is not the seller");
        // A late shipment must not remove an already-earned timeout refund right.
        require(block.timestamp <= o.deadline, "Escrow: shipment deadline passed");
        o.status = Status.Shipped;
        emit OrderShipped(id, msg.sender);
    }

    function confirm(uint256 id) external exists(id) onlyBuyer(id) {
        Order storage o = _orders[id];
        require(o.status == Status.Paid || o.status == Status.Shipped, "Escrow: wrong status");
        o.status = Status.Completed;
        require(IERC20(o.token).transfer(o.seller, o.amount), "Escrow: transfer failed");
        emit OrderCompleted(id);
    }

    function refund(uint256 id) external exists(id) onlyBuyer(id) inStatus(id, Status.Paid) {
        Order storage o = _orders[id];
        require(block.timestamp > o.deadline, "Escrow: deadline not reached");
        o.status = Status.Refunded;
        require(IERC20(o.token).transfer(o.buyer, o.amount), "Escrow: transfer failed");
        emit OrderRefunded(id);
    }

    function dispute(uint256 id) external exists(id) {
        Order storage o = _orders[id];
        require(o.status == Status.Paid || o.status == Status.Shipped, "Escrow: wrong status");
        require(msg.sender == o.buyer || msg.sender == o.seller, "Escrow: caller is not a party");
        o.status = Status.Disputed;
        emit OrderDisputed(id, msg.sender);
    }

    function resolve(uint256 id, bool sellerWins) external exists(id) inStatus(id, Status.Disputed) {
        require(msg.sender == arbiter || msg.sender == secondArbiter, "Escrow: caller is not an arbiter");
        Vote storage vote = _votes[id][msg.sender];
        require(!vote.cast || vote.sellerWins != sellerWins, "Escrow: duplicate vote");
        // A voter may revise a disagreement while unresolved. Only distinct addresses count.
        vote.cast = true;
        vote.sellerWins = sellerWins;
        emit ArbitrationVoted(id, msg.sender, sellerWins);
        Vote memory first = _votes[id][arbiter];
        Vote memory second = _votes[id][secondArbiter];
        if (!first.cast || !second.cast || first.sellerWins != second.sellerWins) return;
        Order storage o = _orders[id];
        o.status = sellerWins ? Status.Completed : Status.Refunded;
        require(IERC20(o.token).transfer(sellerWins ? o.seller : o.buyer, o.amount), "Escrow: transfer failed");
        emit OrderResolved(id, sellerWins);
    }
}
