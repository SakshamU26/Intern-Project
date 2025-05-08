package com.example.demo.service;

import com.example.demo.pojo.OllamaRequest;
import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.SampleData;
import com.example.demo.pojo.VersionData;
import com.example.demo.repository.RecordDataRepository;
import com.example.demo.repository.SampleDataRepository;
import com.example.demo.repository.VersionDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TranslationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TranslationScheduler.class);

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private RecordDataRepository recordDataRepository;

    @Autowired
    private VersionDataRepository versionDataRepository;

    @Autowired
    private SampleDataRepository sampleDataRepository;

    @Scheduled(fixedRate = 20000)
    public void runTranslationJob() {
        List<RecordData> nullData = recordDataRepository.findByFriendlyColumnNameIsNullAndVersionCountLessThan(3);
        for(RecordData curr : nullData) {
            int count = curr.getVersionCount();

            List<SampleData> samples = sampleDataRepository.findByTableNameAndColumnName
                    (curr.getTableName(), curr.getColumnName());

            StringBuilder context = new StringBuilder();
            for (SampleData x : samples) {
                context.append(x.getSampleRow()).append("\n");
            }

            StringBuilder dynamicPromptBuilder = new StringBuilder();
            dynamicPromptBuilder.append("given the following context:\n")
                    .append("- Table: ").append(curr.getTableName()).append("\n")
                    .append("- Column: ").append(curr.getColumnName()).append("\n")
                    .append("- Description: ").append(curr.getDescription()).append("\n")
                    .append("- Example Data: ").append(curr.getData()).append("\n\n");

            if (!context.toString().trim().isEmpty()) {
                dynamicPromptBuilder.append("Some sample data for this column ")
                        .append(curr.getColumnName())
                        .append(" is:-\n")
                        .append(context).append("\n");
            }

            dynamicPromptBuilder.append("Translate the column name '")
                    .append(curr.getColumnName())
                    .append("' into clear and descriptive English Column Name.\n\n");

            String dynamicPrompt = dynamicPromptBuilder.toString();

            logger.info("Generated prompt for column '{}':\n\n{}", curr.getColumnName(), dynamicPrompt);
            OllamaRequest request = new OllamaRequest("gemma3:4b",dynamicPrompt, false);
            String ffn = ollamaService.communicateWithOllama(request).getResponse();

            VersionData t2 = new VersionData(null, curr, count+1, ffn, LocalDateTime.now());
            versionDataRepository.save(t2);
            curr.setVersionCount(count+1);

            recordDataRepository.save(curr);
        }
    }
}
