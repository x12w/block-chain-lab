// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0 <0.9.0;

// Remix: compile this file, then use At Address. Do not deploy an interface.
// POTOS chainId 60600; enhanced contract 0x917Dc833eadBa6544F1fd80498715da39Ac739f4.
interface IRouteABonus {
    struct Order {
        address buyer;
        address seller;
        address token;
        uint256 amount;
        uint256 deadline;
        uint8 status;
        string description;
    }
    function version() external view returns (uint256);
    function arbiter() external view returns (address);
    function secondArbiter() external view returns (address);
    function orderCount() external view returns (uint256);
    function getOrder(uint256 id) external view returns (Order memory);
    function getVotes(uint256 id) external view returns
        (bool firstVoted, bool firstSellerWins, bool secondVoted, bool secondSellerWins);
    function createOrder(address seller, address token, uint256 amount, uint256 deadline,
                         string calldata description) external returns (uint256 id);
    function deposit(uint256 id) external;
    function markShipped(uint256 id) external;
    function confirm(uint256 id) external;
    function refund(uint256 id) external;
    function dispute(uint256 id) external;
    function resolve(uint256 id, bool sellerWins) external;
    event OrderCreated(uint256 indexed id, address indexed buyer, address indexed seller,
                       address token, uint256 amount, uint256 deadline);
    event OrderPaid(uint256 indexed id);
    event OrderShipped(uint256 indexed id, address seller);
    event OrderCompleted(uint256 indexed id);
    event OrderRefunded(uint256 indexed id);
    event OrderDisputed(uint256 indexed id, address by);
    event ArbitrationVoted(uint256 indexed id, address indexed voter, bool sellerWins);
    event OrderResolved(uint256 indexed id, bool sellerWins);
}

// WQLToken: 0xB7dA837F93A5448ca0762B80862d053daf9715B3 (18 decimals).
interface IRouteABonusToken {
    function decimals() external view returns (uint8);
    function balanceOf(address owner) external view returns (uint256);
    function allowance(address owner, address spender) external view returns (uint256);
    function approve(address spender, uint256 amount) external returns (bool);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    event Transfer(address indexed from, address indexed to, uint256 value);
}
