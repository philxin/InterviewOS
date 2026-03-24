package com.philxin.interviewos.repository;

import com.philxin.interviewos.entity.RegistrationInvitation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationInvitationRepository extends JpaRepository<RegistrationInvitation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from RegistrationInvitation invitation where invitation.id = :id")
    Optional<RegistrationInvitation> findByIdForUpdate(@Param("id") UUID id);
}
