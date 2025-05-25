package com.easy.auth.customlogic.repository;



import com.easy.auth.customlogic.model.CustomLogic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomLogicRepository extends JpaRepository<CustomLogic, Long> {
}
