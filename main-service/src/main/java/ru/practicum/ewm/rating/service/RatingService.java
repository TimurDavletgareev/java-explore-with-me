package ru.practicum.ewm.rating.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.error.exception.IncorrectRequestException;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.event.dto.EventMapper;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.participationRequest.model.PartRequestState;
import ru.practicum.ewm.participationRequest.model.ParticipationRequest;
import ru.practicum.ewm.participationRequest.repository.PartRequestRepository;
import ru.practicum.ewm.rating.dto.RatingDto;
import ru.practicum.ewm.rating.dto.RatingMapper;
import ru.practicum.ewm.rating.model.Rating;
import ru.practicum.ewm.rating.repository.RatingRepository;
import ru.practicum.ewm.user.dto.UserMapper;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PartRequestRepository partRequestRepository;

    @Transactional
    public RatingDto setLike(Long eventId, Long userId) {

        log.info("-- Добавление лайка от пользователя id={} событию id={}", userId, eventId);

        return setRating(eventId, userId, 1);
    }

    @Transactional
    public RatingDto setDislike(Long eventId, Long userId) {

        log.info("-- Добавление дизлайка от пользователя id={} событию id={}", userId, eventId);

        return setRating(eventId, userId, -1);
    }

    private RatingDto setRating(Long eventId, Long userId, Integer inputRating) {

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("- Событие с id=" + eventId + " не найдено"));

        User initiator = event.getInitiator();

        //блок проверок
        ParticipationRequest partRequest = partRequestRepository.findByEventAndRequester(eventId, userId)
                .orElseThrow(() ->
                        new IncorrectRequestException("- Запрос пользователя на участие в событии отсутствует"));

        if (!partRequest.getStatus().equals(PartRequestState.CONFIRMED.toString())) {
            throw new IncorrectRequestException("- Запрос пользователя на участие в событии не подтвержден");
        }
        //конец блока проверок

        Optional<Rating> currentRatingOptional = ratingRepository.findByEventIdAndUserId(eventId, userId);

        // если пользователь уже оценивал событие
        if (currentRatingOptional.isPresent()) {

            Rating currentRating = currentRatingOptional.get();

            // если аналогичная оценка от этого пользователя уже была, то повторное проставление убирает текущую оценку
            if (currentRating.getRating().equals(inputRating)) {
                ratingRepository.deleteById(currentRating.getId());
                currentRating.setRating(0);
                event.setRating(event.getRating() + inputRating * -1);
                initiator.setRating(initiator.getRating() + inputRating * -1);
                eventRepository.save(event);
                userRepository.save(initiator);
                log.info("-- Существующая оценка удалёна при повторном добавлении оценки событию");

                // если от этого пользователя уже была противоположная оценка,
                // то меняем её на новую
            } else if (currentRating.getRating().equals(-1 * inputRating)) {
                currentRating.setRating(inputRating);
                currentRating = ratingRepository.save(currentRating);
                event.setRating(event.getRating() + inputRating * 2);
                initiator.setRating(initiator.getRating() + inputRating * 2);
                eventRepository.save(event);
                userRepository.save(initiator);
                log.info("-- Существующая оценка удалёна, новая оценка добавлена: {}", inputRating);
            }

            return RatingMapper.ratingToDto(currentRating);
        }

        // если пользователь ещё не оценивал событие
        Rating newRating = new Rating();
        newRating.setEventId(eventId);
        newRating.setUserId(userId);
        newRating.setRating(inputRating);
        newRating = ratingRepository.save(newRating);
        event.setRating(event.getRating() + inputRating);
        initiator.setRating(initiator.getRating() + inputRating);
        eventRepository.save(event);
        userRepository.save(initiator);

        log.info("-- Новая оценка добавлена: {}", inputRating);

        return RatingMapper.ratingToDto(newRating);
    }

    /*
        Метод пагинации
     */
    private PageRequest getPageRequest(int from, int size) {

        if (size > 0 && from >= 0) {
            int page = from / size;
            return PageRequest.of(page, size, Sort.by("rating").descending());
        } else {
            throw new IncorrectRequestException("- Размер страницы должен быть > 0, 'from' должен быть >= 0");
        }
    }

    public List<UserShortDto> getUsersRating(int from, int size) {

        log.info("-- Возвращение рейтинга пользователей с параметрами: from={}, size={}", from, size);

        PageRequest pageRequest = getPageRequest(from, size);

        Iterable<User> foundUsers = userRepository.findAll(pageRequest);

        List<UserShortDto> listToReturn = UserMapper.userToShortDto(foundUsers);

        log.info("-- Рейтинг пользователей возвращен, его размер: {}", listToReturn.size());

        return listToReturn;
    }

    public List<EventShortDto> getEventsRating(int from, int size) {

        log.info("-- Возвращение рейтинга событий с параметрами: from={}, size={}", from, size);

        PageRequest pageRequest = getPageRequest(from, size);

        Iterable<Event> foundEvents = eventRepository.findAll(pageRequest);

        List<EventShortDto> listToReturn = EventMapper.eventToShortDto(foundEvents);

        log.info("-- Рейтинг событий возвращен, его размер: {}", listToReturn.size());

        return listToReturn;
    }
}
