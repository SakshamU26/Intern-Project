package com.example.demo.repository;

import com.example.demo.pojo.RequestData;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RequestDataRepository extends JpaRepository<RequestData,Long> {
}
