package com.example.demo.dao;

import com.example.demo.pojo.RequestData;
import com.example.demo.repository.RequestDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequestDataDAO {

    private final RequestDataRepository requestDataRepository;

    public RequestDataDAO(RequestDataRepository requestDataRepository) {
        this.requestDataRepository = requestDataRepository;
    }

    public void saveAll(List<RequestData> requestDataList) {
        requestDataRepository.saveAll(requestDataList);
    }
}