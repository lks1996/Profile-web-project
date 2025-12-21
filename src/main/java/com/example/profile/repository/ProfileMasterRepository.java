package com.example.profile.repository;

import com.example.profile.model.ProfileMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileMasterRepository  extends JpaRepository<ProfileMaster, Long> {
}
