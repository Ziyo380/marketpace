package com.marketplace.promotion.repository;

import com.marketplace.promotion.entity.Promocode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromocodeRepository extends JpaRepository<Promocode, UUID> {
    Optional<Promocode> findByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promocode p where p.code = :code")
    Optional<Promocode> findByCodeForUpdate(@Param("code") String code);
}
