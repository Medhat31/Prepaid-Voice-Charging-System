import java.math.BigDecimal;
import java.util.List;

public class PhoneBookService implements IPhoneBookService {

    private final IPhoneBookRepository repository;

    public PhoneBookService(IPhoneBookRepository repository) {
        this.repository = repository;
    }

    @Override
    public void addNumber(String msisdn, BigDecimal initialBalance) {
        if (msisdn == null || msisdn.isBlank()) {
            throw new IllegalArgumentException("MSISDN cannot be empty.");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        if (repository.exists(msisdn)) {
            throw new IllegalStateException("Number already exists: " + msisdn);
        }
        repository.addNumber(msisdn, initialBalance);
    }

    @Override
    public void deleteNumber(String msisdn) {
        if (!repository.exists(msisdn)) {
            throw new IllegalStateException("Number not found: " + msisdn);
        }
        repository.deleteNumber(msisdn);
    }

    @Override
    public void updateBalance(String msisdn, BigDecimal newBalance) {
        if (newBalance == null || newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        if (!repository.exists(msisdn)) {
            throw new IllegalStateException("Number not found: " + msisdn);
        }
        repository.updateBalance(msisdn, newBalance);
    }

    @Override
    public List<PhoneRecord> getAllNumbers() {
        return repository.getAllNumbers();
    }
}
