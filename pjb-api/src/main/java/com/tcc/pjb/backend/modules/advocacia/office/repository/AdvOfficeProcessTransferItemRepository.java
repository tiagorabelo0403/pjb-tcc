package com.tcc.pjb.backend.modules.advocacia.office.repository;

import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessTransferItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdvOfficeProcessTransferItemRepository extends JpaRepository<AdvOfficeProcessTransferItem, Long> {

    @Query("""
            select i from AdvOfficeProcessTransferItem i
            join fetch i.processo
            where i.transfer.id = :transferId
            order by i.id asc
            """)
    List<AdvOfficeProcessTransferItem> findByTransferId(Long transferId);
}
