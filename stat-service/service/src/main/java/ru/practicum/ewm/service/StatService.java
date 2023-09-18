package ru.practicum.ewm.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.NewStatDto;
import ru.practicum.ewm.dto.StatDtoToReturn;
import ru.practicum.ewm.dto.StatMapper;
import ru.practicum.ewm.dto.StatRecordDto;
import ru.practicum.ewm.model.StatRecord;
import ru.practicum.ewm.repository.StatRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class StatService {

    private final StatRepository statRepository;
    //private static final String APP_NAME = "ewm-main-service";

    public StatRecordDto add(NewStatDto newStatDto) {

        log.info("-- Добавление записи: {}", newStatDto);

        StatRecordDto recordDto = StatMapper.newStatToRecordDto(newStatDto);

        StatRecord record = statRepository.save(new StatRecord(recordDto));

        recordDto.setId(record.getId());

        log.info("-- Запись добавлена: {}", recordDto);

        return recordDto;
    }

    public List<StatDtoToReturn> get(String appName, String start, String end, String[] uris, boolean unique) {

        log.info("-- Возвращение списка записей: app={}, start={}, end={}, uris={}, unique={}",
                appName,
                start,
                end,
                Arrays.toString(uris),
                unique
        );

        List<StatDtoToReturn> listToReturn = new ArrayList<>();

        start = java.net.URLDecoder.decode(start, StandardCharsets.UTF_8);
        end = java.net.URLDecoder.decode(end, StandardCharsets.UTF_8);

        LocalDateTime startTime =
                LocalDateTime.parse(start, StatMapper.DATE_TIME_FORMATTER);
        LocalDateTime endTime =
                LocalDateTime.parse(end, StatMapper.DATE_TIME_FORMATTER);

        int hits;

        if (uris == null) {

            uris = statRepository.listOfAllUris(appName, startTime, endTime).toArray((new String[0]));

        }

        for (String uri : uris) {

            if (unique) {
                hits = statRepository.sizeOfUniqueIpRecordsListByUri(appName, startTime, endTime, uri);
            } else {
                hits = statRepository.sizeOfAllRecordsListByUri(appName, startTime, endTime, uri);
            }

            listToReturn.add(new StatDtoToReturn(appName, uri, hits));
        }

        listToReturn.sort(Comparator.reverseOrder());

        log.info("-- Список возвращен, его размер: {}", listToReturn.size());

        return listToReturn;
    }
}