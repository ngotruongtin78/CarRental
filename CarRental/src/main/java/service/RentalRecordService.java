package CarRental.example.service;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RentalRecordService {

    private final RentalRecordRepository repo;

    public RentalRecordService(RentalRecordRepository repo) {
        this.repo = repo;
    }

    public RentalRecord saveRecord(RentalRecord record) {
        return repo.save(record);
    }

    public List<RentalRecord> getHistoryByUsername(String username) {
        return repo.findByUsername(username);
    }

    public List<RentalRecord> getAll() {
        return repo.findAll();
    }

    public RentalRecord getById(String id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }
}
