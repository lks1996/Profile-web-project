package com.example.profile.repository;

import com.example.profile.model.ProfileMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileMasterRepository  extends JpaRepository<ProfileMaster, Long> {

    Optional<ProfileMaster> findByIsActiveTrue();
}
