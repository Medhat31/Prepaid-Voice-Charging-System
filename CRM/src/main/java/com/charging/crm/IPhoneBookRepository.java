import java.math.BigDecimal;
import java.util.List;

public interface IPhoneBookRepository {
    void addNumber(String msisdn, BigDecimal initialBalance);
    void deleteNumber(String msisdn);
    void updateBalance(String msisdn, BigDecimal newBalance);
    List<PhoneRecord> getAllNumbers();
    boolean exists(String msisdn);
    BigDecimal getBalance(String msisdn);
}
