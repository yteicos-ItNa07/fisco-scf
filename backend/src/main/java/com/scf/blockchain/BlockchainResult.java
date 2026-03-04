package com.scf.blockchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区块链交易结果封装，包含交易哈希和业务返回值
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockchainResult<T> {

    private String txHash;
    private T value;

    public static BlockchainResult<Void> ofVoid(String txHash) {
        return new BlockchainResult<>(txHash, null);
    }
}
