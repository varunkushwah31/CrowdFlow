package com.civic.waterwatch.ward;

import com.civic.waterwatch.ward.model.MunicipalWard;
import com.civic.waterwatch.ward.repository.MunicipalWardRepository;
import com.civic.waterwatch.ward.service.WardRoutingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class WardRoutingTest {

    @Test
    @DisplayName("Should route Indian coordinates directly into correct Delhi Jal Board ward envelope")
    void testDirectWardRouting() {
        MunicipalWard ward85 = new MunicipalWard(
                85, "Ward 85 - Karol Bagh", "Central Zone",
                "Delhi Jal Board (DJB)", "Delhi / India", "110005",
                "Shri Alok Sharma", "EE - Water",
                "ee.karolbagh@delhijalboard.nic.in", "+91 98110 23412", "1916",
                "http://test/ward85",
                28.6300, 28.6600, 77.1800, 77.2150,
                28.6450, 77.1950
        );

        MunicipalWard ward142 = new MunicipalWard(
                142, "Ward 142 - Lajpat Nagar", "South Zone",
                "Delhi Jal Board (DJB)", "Delhi / India", "110024",
                "Smt. Sunita Rao", "AEE - South",
                "aee.lajpatnagar@delhijalboard.nic.in", "+91 98711 54321", "1916",
                "http://test/ward142",
                28.5550, 28.5850, 77.2250, 77.2550,
                28.5700, 77.2400
        );

        List<MunicipalWard> allWards = List.of(ward85, ward142);

        // Test stub avoiding JDK 25 ByteBuddy dynamic agent instrumentation
        MunicipalWardRepository stubRepo = new MunicipalWardRepository() {
            @Override
            public Optional<MunicipalWard> findByWardNumber(Integer wardNumber) {
                return allWards.stream().filter(w -> w.getWardNumber().equals(wardNumber)).findFirst();
            }

            @Override
            public List<MunicipalWard> findAll() {
                return allWards;
            }

            @Override
            public long count() {
                return allWards.size();
            }

            @Override public void flush() {}
            @Override public <S extends MunicipalWard> S saveAndFlush(S entity) { return entity; }
            @Override public <S extends MunicipalWard> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            @Override public void deleteAllInBatch(Iterable<MunicipalWard> entities) {}
            @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
            @Override public void deleteAllInBatch() {}
            @Override public MunicipalWard getOne(Long id) { return null; }
            @Override public MunicipalWard getById(Long id) { return null; }
            @Override public MunicipalWard getReferenceById(Long id) { return null; }
            @Override public <S extends MunicipalWard> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
            @Override public <S extends MunicipalWard> List<S> findAll(Example<S> example) { return List.of(); }
            @Override public <S extends MunicipalWard> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
            @Override public <S extends MunicipalWard> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
            @Override public <S extends MunicipalWard> long count(Example<S> example) { return 0; }
            @Override public <S extends MunicipalWard> boolean exists(Example<S> example) { return false; }
            @Override public <S extends MunicipalWard, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            @Override public <S extends MunicipalWard> S save(S entity) { return entity; }
            @Override public <S extends MunicipalWard> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            @Override public Optional<MunicipalWard> findById(Long id) { return Optional.empty(); }
            @Override public boolean existsById(Long id) { return false; }
            @Override public List<MunicipalWard> findAllById(Iterable<Long> ids) { return List.of(); }
            @Override public void deleteById(Long id) {}
            @Override public void delete(MunicipalWard entity) {}
            @Override public void deleteAllById(Iterable<? extends Long> ids) {}
            @Override public void deleteAll(Iterable<? extends MunicipalWard> entities) {}
            @Override public void deleteAll() {}
            @Override public List<MunicipalWard> findAll(Sort sort) { return allWards; }
            @Override public Page<MunicipalWard> findAll(Pageable pageable) { return null; }
        };

        WardRoutingService service = new WardRoutingService(stubRepo);

        // Coordinate inside Ward 85 (Karol Bagh: 28.6445, 77.1950)
        MunicipalWard matched = service.routeToWard(28.6445, 77.1950);
        assertNotNull(matched);
        assertEquals(85, matched.getWardNumber());
        assertEquals("Delhi Jal Board (DJB)", matched.getMunicipalBody());

        // Coordinate inside Ward 142 (Lajpat Nagar: 28.5700, 77.2400)
        MunicipalWard matched2 = service.routeToWard(28.5700, 77.2400);
        assertNotNull(matched2);
        assertEquals(142, matched2.getWardNumber());
    }
}
