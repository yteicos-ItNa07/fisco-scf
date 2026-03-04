package com.scf.repository;

import com.scf.entity.User;
import com.scf.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByEnterprise(Enterprise enterprise);

    boolean existsByUsername(String username);
}
