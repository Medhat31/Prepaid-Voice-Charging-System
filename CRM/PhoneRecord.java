public class PhoneRecord {
    private final String msisdn;
    private final java.math.BigDecimal balance;

    public PhoneRecord(String msisdn, java.math.BigDecimal balance) {
        this.msisdn = msisdn;
        this.balance = balance;
    }

    public String getMsisdn() { return msisdn; }
    public java.math.BigDecimal getBalance() { return balance; }
}
