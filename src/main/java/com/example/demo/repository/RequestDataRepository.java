package com.example.demo.repository;

import com.example.demo.pojo.RequestData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestDataRepository extends JpaRepository<RequestData,Long> {
    List<RequestData> findAllByRequestId(Long id);
}
