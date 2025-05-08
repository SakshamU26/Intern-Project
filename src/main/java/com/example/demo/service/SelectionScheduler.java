package com.example.demo.service;

import com.example.demo.pojo.*;
import com.example.demo.repository.RecordDataRepository;
import com.example.demo.repository.SampleDataRepository;
import com.example.demo.repository.VersionDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class SelectionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SelectionScheduler.class);

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private RecordDataService recordDataService;

    @Autowired
    private RecordDataRepository recordDataRepository;

    @Autowired
    private VersionDataRepository versionDataRepository;

    @Autowired
    private SampleDataRepository sampleDataRepository;

    @Transactional
    @Scheduled(fixedRate = 65000)
    public void runSelectionJob() {

        boolean anyProcessed = false;

        List<VersionData> nullData = versionDataRepository.findEntriesWithNullFriendlyNameAndExactly3Versions();
        nullData.sort(Comparator.comparing(v -> v.getRecordDataId().getId()));

        Map<Long, List<String>> suggestionMap = new HashMap<>();
        Map<Long, RecordData> recordDataMap = new HashMap<>();

        for (VersionData data : nullData) {
            Long tableDataId = data.getRecordDataId().getId();
            String suggestion = data.getSuggestions();

            suggestionMap.computeIfAbsent(tableDataId, k -> new ArrayList<>()).add(suggestion);
            recordDataMap.putIfAbsent(tableDataId, data.getRecordDataId());
        }


        List<ProcessedColumn> processedColumns = new ArrayList<>();

        for (Map.Entry<Long, List<String>> entry : suggestionMap.entrySet()) {

            Long id = entry.getKey();
            List<String> suggestions = suggestionMap.get(id);
            RecordData recordData = recordDataMap.get(id);
            List<SampleData> samples = sampleDataRepository.findByTableNameAndColumnName
                    (recordData.getTableName(), recordData.getColumnName());

            StringBuilder context = new StringBuilder();
            for (SampleData x : samples) {
                context.append(x.getSampleRow()).append("\n");
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are given the following information to help choose the best column name:\n")
                    .append("- Table Name: ").append(recordData.getTableName()).append("\n")
                    .append("- Column Name: ").append(recordData.getColumnName()).append("\n")
                    .append("- Description: ").append(recordData.getDescription()).append("\n")
                    .append("- Example Data: ").append(recordData.getData()).append("\n\n");

            if (!context.toString().trim().isEmpty()) {
                promptBuilder.append("Some sample columns in the same table with its data are:-\n")
                        .append(context).append("\n");
            }

            promptBuilder.append("Here are three candidate Column Name suggestions:\n")
                    .append("1. ").append(suggestions.get(0)).append("\n")
                    .append("2. ").append(suggestions.get(1)).append("\n")
                    .append("3. ").append(suggestions.get(2)).append("\n\n")
                    .append("Choose the best Column Name accordingly (only return the best one)");

            String prompt = promptBuilder.toString();

            logger.info("Generated prompt for column '{}':\n\n{}", recordData.getColumnName(), prompt);
            OllamaRequest request = new OllamaRequest("gemma3:4b", prompt, false);
            String bestSuggestion = ollamaService.selectBestSuggestionWithOllama(request).getResponse();

            int selected = -1;
            for(int i=0; i<3; i++) {
                if(suggestions.get(i).trim().equalsIgnoreCase(bestSuggestion.trim())) {
                    selected = i+1;
                    break;
                }
            }
            RecordData tableData = recordDataRepository.getReferenceById(id);
            tableData.setFriendlyColumnName(bestSuggestion);
            tableData.setVersionSelected(selected);
            recordDataRepository.save(tableData);

            processedColumns.add(new ProcessedColumn(tableData.getTableName(), tableData.getColumnName()));
            anyProcessed = true;
        }

        if(anyProcessed) {

            // Email service
            recordDataService.sendEmail(processedColumns);

            System.out.println("***********");
            System.out.println("Email sent!");
            System.out.println("***********");
        }
    }
}
