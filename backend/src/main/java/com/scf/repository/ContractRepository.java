package com.scf.repository;

import com.scf.entity.Contract;
import com.scf.entity.Enterprise;
import com.scf.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /**
     * 根据文件哈希查找合同
     */
    Optional<Contract> findByFileHash(String fileHash);

    /**
     * 根据应收账款查找关联的合同
     */
    List<Contract> findByReceivable(Receivable receivable);

    /**
     * 根据上传企业查找合同
     */
    List<Contract> findByUploaderEnterprise(Enterprise enterprise);

    /**
     * 根据应收账款ID查找合同
     */
    @Query("SELECT c FROM Contract c WHERE c.receivable.id = :receivableId")
    List<Contract> findByReceivableId(Long receivableId);

    /**
     * 检查哈希是否已存在
     */
    boolean existsByFileHash(String fileHash);
}
