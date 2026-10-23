package com.apnabank;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@SpringBootApplication
@EnableScheduling
public class ApnaBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApnaBankApplication.class, args);
    }
}

// ==================== SECURITY ====================
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(f -> f.disable()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/i18n/**", "/api/auth/**", "/api/market/**").permitAll()
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );
        return http.build();
    }
}

// ==================== DTOs ====================
class DTOs {
    public record RegisterRequest(String fullName, String username, String password, String preferredLanguage) {}
    public record LoginRequest(String username, String password) {}
    public record AmountRequest(BigDecimal amount, String note) {}
    public record TransferRequest(String recipientUsername, BigDecimal amount, String note) {}
    public record TradeRequest(String symbol, BigDecimal quantity, BigDecimal inrAmount) {}
    public record MessageRequest(String recipientUsername, String body) {}
    public record LanguageRequest(String language) {}
}

// ==================== ENTITIES ====================
@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String passwordHash;
    private String preferredLanguage = "en";
    private String accountStatus = "ACTIVE";
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

@Entity
@Table(name = "accounts")
class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    private String accountType = "ROYAL_SAVINGS";
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.valueOf(500000.00); // 5 Lakh Starting Royal Capital
    private String currency = "INR";
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getCurrency() { return currency; }
}

@Entity
@Table(name = "transactions")
class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private String counterpartyUsername;
    private String description;
    private String status = "COMPLETED";
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public String getCounterpartyUsername() { return counterpartyUsername; }
    public void setCounterpartyUsername(String counterpartyUsername) { this.counterpartyUsername = counterpartyUsername; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

@Entity
@Table(name = "loans")
class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    private BigDecimal principal;
    private BigDecimal accruedInterest = BigDecimal.ZERO;
    private BigDecimal interestRate;
    private String status = "ACTIVE";
    private OffsetDateTime issuedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }
    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

@Entity
@Table(name = "market_assets")
class MarketAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String symbol;
    private String name;
    private String category;
    private BigDecimal price;
    private BigDecimal volatility;
    private String tradingSchedule = "24_7";
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getVolatility() { return volatility; }
    public String getTradingSchedule() { return tradingSchedule; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

@Entity
@Table(name = "holdings", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "asset_id"})})
class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id", nullable = false)
    private MarketAsset asset;
    
    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal averagePurchasePrice = BigDecimal.ZERO;

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public MarketAsset getAsset() { return asset; }
    public void setAsset(MarketAsset asset) { this.asset = asset; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAveragePurchasePrice() { return averagePurchasePrice; }
    public void setAveragePurchasePrice(BigDecimal averagePurchasePrice) { this.averagePurchasePrice = averagePurchasePrice; }
}

@Entity
@Table(name = "messages")
class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    @Column(nullable = false, length = 2000)
    private String body;
    private boolean isRead = false;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

@Entity
@Table(name = "government_ledger")
class GovernmentLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private UUID transactionId;
    private BigDecimal taxAmount;
    private String sourceUsername;
    private String recipientUsername;

    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public void setSourceUsername(String sourceUsername) { this.sourceUsername = sourceUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }
}

// ==================== REPOSITORIES ====================
@Repository interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
@Repository interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUser(User user);
}
@Repository interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
@Repository interface LoanRepository extends JpaRepository<Loan, UUID> {
    List<Loan> findByUserAndStatus(User user, String status);
    List<Loan> findByUserOrderByIssuedAtDesc(User user);
}
@Repository interface MarketAssetRepository extends JpaRepository<MarketAsset, UUID> {
    Optional<MarketAsset> findBySymbol(String symbol);
    List<MarketAsset> findByCategory(String category);
}
@Repository interface HoldingRepository extends JpaRepository<Holding, UUID> {
    List<Holding> findByUser(User user);
    Optional<Holding> findByUserAndAsset(User user, MarketAsset asset);
}
@Repository interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByRecipientOrderByCreatedAtDesc(User recipient);
}
@Repository interface GovernmentLedgerRepository extends JpaRepository<GovernmentLedger, UUID> {}

// ==================== SERVICES ====================
@Service
class BankingService {
    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;
    private final UserRepository userRepo;
    private final GovernmentLedgerRepository govRepo;

    public BankingService(AccountRepository accountRepo, TransactionRepository txRepo, UserRepository userRepo, GovernmentLedgerRepository govRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.govRepo = govRepo;
    }

    public Account getAccountForUser(User user) {
        return accountRepo.findByUser(user).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    public Account deposit(User user, BigDecimal amount, String note) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        Account account = getAccountForUser(user);
        account.setBalance(account.getBalance().add(amount));
        accountRepo.save(account);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType("DEPOSIT");
        tx.setAmount(amount);
        tx.setDescription(note != null && !note.isBlank() ? note : "Royal Capital Deposit");
        txRepo.save(tx);
        return account;
    }

    @Transactional
    public Account withdraw(User user, BigDecimal amount, String note) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        Account account = getAccountForUser(user);
        if (account.getBalance().compareTo(amount) < 0) throw new IllegalStateException("Insufficient royal funds");
        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType("WITHDRAW");
        tx.setAmount(amount);
        tx.setDescription(note != null && !note.isBlank() ? note : "Cash Withdrawal");
        txRepo.save(tx);
        return account;
    }

    @Transactional
    public void transfer(User sender, String recipientUsername, BigDecimal amount, String note) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (sender.getUsername().equalsIgnoreCase(recipientUsername)) throw new IllegalArgumentException("Cannot transfer to yourself");

        User recipient = userRepo.findByUsername(recipientUsername).orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        Account senderAccount = getAccountForUser(sender);
        Account recipientAccount = getAccountForUser(recipient);

        BigDecimal taxAmount = amount.multiply(new BigDecimal("0.06")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeduction = amount.add(taxAmount);

        if (senderAccount.getBalance().compareTo(totalDeduction) < 0) {
            throw new IllegalStateException("Insufficient funds! Required: ₹" + totalDeduction + " (including 6% royal tax)");
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(totalDeduction));
        accountRepo.save(senderAccount);

        recipientAccount.setBalance(recipientAccount.getBalance().add(amount));
        accountRepo.save(recipientAccount);

        Transaction sTx = new Transaction();
        sTx.setAccount(senderAccount);
        sTx.setTransactionType("TRANSFER_OUT");
        sTx.setAmount(amount);
        sTx.setTaxAmount(taxAmount);
        sTx.setCounterpartyUsername(recipient.getUsername());
        sTx.setDescription(note != null && !note.isBlank() ? note : "Transfer to " + recipient.getUsername());
        txRepo.save(sTx);

        Transaction rTx = new Transaction();
        rTx.setAccount(recipientAccount);
        rTx.setTransactionType("TRANSFER_IN");
        rTx.setAmount(amount);
        rTx.setCounterpartyUsername(sender.getUsername());
        rTx.setDescription("Received from " + sender.getUsername());
        txRepo.save(rTx);

        GovernmentLedger gov = new GovernmentLedger();
        gov.setTransactionId(sTx.getId());
        gov.setTaxAmount(taxAmount);
        gov.setSourceUsername(sender.getUsername());
        gov.setRecipientUsername(recipient.getUsername());
        govRepo.save(gov);
    }

    public List<Transaction> getTransactions(User user) {
        return txRepo.findByAccountIdOrderByCreatedAtDesc(getAccountForUser(user).getId());
    }
}

@Service
class InvestmentService {
    private final MarketAssetRepository assetRepo;
    private final HoldingRepository holdingRepo;
    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    public InvestmentService(MarketAssetRepository assetRepo, HoldingRepository holdingRepo, AccountRepository accountRepo, TransactionRepository txRepo) {
        this.assetRepo = assetRepo;
        this.holdingRepo = holdingRepo;
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
    }

    @Transactional
    public void buyAsset(User user, String symbol, BigDecimal quantity, BigDecimal inrAmount) {
        MarketAsset asset = assetRepo.findBySymbol(symbol).orElseThrow(() -> new IllegalArgumentException("Asset not found: " + symbol));

        // Support buying either by exact units OR by INR amount
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            if (inrAmount != null && inrAmount.compareTo(BigDecimal.ZERO) > 0) {
                quantity = inrAmount.divide(asset.getPrice(), 6, RoundingMode.HALF_UP);
            } else {
                throw new IllegalArgumentException("Specify either a valid quantity or an INR amount to invest");
            }
        }

        // Fractional ownership rule: Minimum 0.0001 unit
        if (quantity.compareTo(new BigDecimal("0.0001")) < 0) {
            throw new IllegalArgumentException("Minimum fractional trade is 0.0001 units");
        }

        BigDecimal totalCost = asset.getPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        Account account = accountRepo.findByUser(user).orElseThrow();

        if (account.getBalance().compareTo(totalCost) < 0) {
            throw new IllegalStateException("Insufficient balance. Required: ₹" + totalCost + " for " + quantity + " units");
        }

        account.setBalance(account.getBalance().subtract(totalCost));
        accountRepo.save(account);

        Holding holding = holdingRepo.findByUserAndAsset(user, asset).orElse(new Holding());
        if (holding.getId() == null) {
            holding.setUser(user);
            holding.setAsset(asset);
            holding.setQuantity(quantity);
            holding.setAveragePurchasePrice(asset.getPrice());
        } else {
            BigDecimal existingTotal = holding.getQuantity().multiply(holding.getAveragePurchasePrice());
            BigDecimal newQty = holding.getQuantity().add(quantity);
            holding.setQuantity(newQty);
            holding.setAveragePurchasePrice(existingTotal.add(totalCost).divide(newQty, 2, RoundingMode.HALF_UP));
        }
        holdingRepo.save(holding);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType("INVESTMENT");
        tx.setAmount(totalCost);
        tx.setDescription("Bought " + quantity.stripTrailingZeros().toPlainString() + " units of " + asset.getSymbol() + " (" + asset.getName() + ")");
        txRepo.save(tx);
    }

    @Transactional
    public void sellAsset(User user, String symbol, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(new BigDecimal("0.0001")) < 0) {
            throw new IllegalArgumentException("Minimum fractional sale is 0.0001 units");
        }
        MarketAsset asset = assetRepo.findBySymbol(symbol).orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        Holding holding = holdingRepo.findByUserAndAsset(user, asset)
                .orElseThrow(() -> new IllegalStateException("You do not hold any units of " + symbol));

        if (holding.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalStateException("Overselling rejected! You hold " + holding.getQuantity().stripTrailingZeros().toPlainString() + " units");
        }

        BigDecimal proceeds = asset.getPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        Account account = accountRepo.findByUser(user).orElseThrow();

        account.setBalance(account.getBalance().add(proceeds));
        accountRepo.save(account);

        BigDecimal rem = holding.getQuantity().subtract(quantity);
        if (rem.compareTo(new BigDecimal("0.000001")) <= 0) {
            holdingRepo.delete(holding);
        } else {
            holding.setQuantity(rem);
            holdingRepo.save(holding);
        }

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType("INVESTMENT");
        tx.setAmount(proceeds);
        tx.setDescription("Sold " + quantity.stripTrailingZeros().toPlainString() + " units of " + asset.getSymbol() + " for ₹" + proceeds);
        txRepo.save(tx);
    }

    public List<Holding> getHoldings(User user) { return holdingRepo.findByUser(user); }
    public List<MarketAsset> getAllAssets() { return assetRepo.findAll(); }
}

@Service
class LoanService {
    private final LoanRepository loanRepo;
    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    public LoanService(LoanRepository loanRepo, AccountRepository accountRepo, TransactionRepository txRepo) {
        this.loanRepo = loanRepo;
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
    }

    public int calculateCreditScore(User user) {
        int score = 720;
        List<Loan> active = loanRepo.findByUserAndStatus(user, "ACTIVE");
        if (active.isEmpty()) score += 60;
        else score -= 40;
        return Math.min(850, Math.max(300, score));
    }

    @Transactional
    public Loan applyForLoan(User user, BigDecimal requestedAmount) {
        if (requestedAmount.compareTo(BigDecimal.valueOf(1000)) < 0 || requestedAmount.compareTo(BigDecimal.valueOf(5000000)) > 0) {
            throw new IllegalArgumentException("Royal loan request must be between ₹1,000 and ₹50,00,000");
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setPrincipal(requestedAmount);
        loan.setAccruedInterest(BigDecimal.ZERO);
        loan.setInterestRate(new BigDecimal("7.50")); // 7.5% Royal Prime Rate
        loan.setStatus("ACTIVE");
        loanRepo.save(loan);

        Account account = accountRepo.findByUser(user).orElseThrow();
        account.setBalance(account.getBalance().add(requestedAmount));
        accountRepo.save(account);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType("LOAN_DISBURSEMENT");
        tx.setAmount(requestedAmount);
        tx.setDescription("Disbursed Royal Loan #" + loan.getId().toString().substring(0, 8));
        txRepo.save(tx);
        return loan;
    }

    @Transactional
    public void repayLoan(User user, UUID loanId, BigDecimal amount) {
        Loan loan = loanRepo.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (!loan.getUser().getId().equals(user.getId())) throw new IllegalStateException("Unauthorized");

        BigDecimal totalOwed = loan.getPrincipal().add(loan.getAccruedInterest());
        if (amount.compareTo(totalOwed) > 0) amount = totalOwed;

        Account account = accountRepo.findByUser(user).orElseThrow();
        if (account.getBalance().compareTo(amount) < 0) throw new IllegalStateException("Insufficient balance to repay");

        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);

        loan.setPrincipal(loan.getPrincipal().subtract(amount));
        if (loan.getPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setPrincipal(BigDecimal.ZERO);
            loan.setStatus("CLOSED");
        }
        loanRepo.save(loan);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setTransactionType("LOAN_REPAYMENT");
        tx.setAmount(amount);
        tx.setDescription("Royal Loan Repayment #" + loan.getId().toString().substring(0, 8));
        txRepo.save(tx);
    }

    public List<Loan> getUserLoans(User user) { return loanRepo.findByUserOrderByIssuedAtDesc(user); }
}

@Service
class MarketSchedulerService {
    private final MarketAssetRepository assetRepo;
    private final Random random = new Random();

    public MarketSchedulerService(MarketAssetRepository assetRepo) {
        this.assetRepo = assetRepo;
    }

    // 24/7 Live Dynamic Market Price Fluctuation
    @Scheduled(fixedRate = 8000)
    public void updateMarketPrices() {
        List<MarketAsset> assets = assetRepo.findAll();
        for (MarketAsset asset : assets) {
            double vol = asset.getVolatility().doubleValue();
            double delta = (random.nextDouble() * 2 * vol) - vol;
            BigDecimal currentPrice = asset.getPrice();
            BigDecimal change = currentPrice.multiply(BigDecimal.valueOf(delta));
            BigDecimal newPrice = currentPrice.add(change).setScale(2, RoundingMode.HALF_UP);
            if (newPrice.compareTo(BigDecimal.ONE) < 0) newPrice = BigDecimal.ONE;
            asset.setPrice(newPrice);
            asset.setUpdatedAt(OffsetDateTime.now());
            assetRepo.save(asset);
        }
    }
}

// ==================== REST CONTROLLERS ====================
@RestController @RequestMapping("/api/auth")
class AuthController {
    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository userRepo, AccountRepository accountRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.accountRepo = accountRepo;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody DTOs.RegisterRequest req, HttpServletRequest request) {
        if (userRepo.existsByUsername(req.username())) return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
        User user = new User();
        user.setFullName(req.fullName());
        user.setUsername(req.username().toLowerCase().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setPreferredLanguage(req.preferredLanguage() != null ? req.preferredLanguage() : "en");
        userRepo.save(user);

        Account acc = new Account();
        acc.setUser(user);
        acc.setBalance(BigDecimal.valueOf(500000.00)); // ₹5,00,000 Initial Royal Balance
        accountRepo.save(acc);

        authenticate(user, request);
        return ResponseEntity.ok(Map.of("message", "Registered", "user", user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody DTOs.LoginRequest req, HttpServletRequest request) {
        User user = userRepo.findByUsername(req.username().toLowerCase().trim()).orElse(null);
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        authenticate(user, request);
        return ResponseEntity.ok(Map.of("message", "Logged in", "username", user.getUsername(), "language", user.getPreferredLanguage()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of("username", user.getUsername(), "fullName", user.getFullName(), "language", user.getPreferredLanguage()));
    }

    private void authenticate(User user, HttpServletRequest request) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
    }
}

@RestController @RequestMapping("/api/banking")
class BankingController {
    private final BankingService bankingService;
    private final UserRepository userRepo;

    public BankingController(BankingService bankingService, UserRepository userRepo) {
        this.bankingService = bankingService;
        this.userRepo = userRepo;
    }

    private User getUser(Authentication auth) { return userRepo.findByUsername(auth.getName()).orElseThrow(); }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(Authentication auth) {
        return ResponseEntity.ok(Map.of("balance", bankingService.getAccountForUser(getUser(auth)).getBalance()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(Authentication auth, @RequestBody DTOs.AmountRequest req) {
        return ResponseEntity.ok(Map.of("newBalance", bankingService.deposit(getUser(auth), req.amount(), req.note()).getBalance()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(Authentication auth, @RequestBody DTOs.AmountRequest req) {
        return ResponseEntity.ok(Map.of("newBalance", bankingService.withdraw(getUser(auth), req.amount(), req.note()).getBalance()));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(Authentication auth, @RequestBody DTOs.TransferRequest req) {
        bankingService.transfer(getUser(auth), req.recipientUsername(), req.amount(), req.note());
        return ResponseEntity.ok(Map.of("message", "Royal Transfer successfully completed (6% tax included)"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(Authentication auth) {
        return ResponseEntity.ok(bankingService.getTransactions(getUser(auth)));
    }
}

@RestController @RequestMapping("/api/investments")
class InvestmentController {
    private final InvestmentService invService;
    private final UserRepository userRepo;

    public InvestmentController(InvestmentService invService, UserRepository userRepo) {
        this.invService = invService;
        this.userRepo = userRepo;
    }

    @GetMapping("/portfolio")
    public ResponseEntity<?> getPortfolio(Authentication auth) {
        return ResponseEntity.ok(invService.getHoldings(userRepo.findByUsername(auth.getName()).orElseThrow()));
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buy(Authentication auth, @RequestBody DTOs.TradeRequest req) {
        invService.buyAsset(userRepo.findByUsername(auth.getName()).orElseThrow(), req.symbol(), req.quantity(), req.inrAmount());
        return ResponseEntity.ok(Map.of("message", "Fractional order executed successfully"));
    }

    @PostMapping("/sell")
    public ResponseEntity<?> sell(Authentication auth, @RequestBody DTOs.TradeRequest req) {
        invService.sellAsset(userRepo.findByUsername(auth.getName()).orElseThrow(), req.symbol(), req.quantity());
        return ResponseEntity.ok(Map.of("message", "Sale executed successfully"));
    }
}

@RestController @RequestMapping("/api/market")
class MarketController {
    private final InvestmentService invService;

    public MarketController(InvestmentService invService) { this.invService = invService; }

    @GetMapping
    public ResponseEntity<?> getMarket() { return ResponseEntity.ok(invService.getAllAssets()); }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of("status", "24_7_ACTIVE", "trading_allowed", true));
    }
}

@RestController @RequestMapping("/api/loans")
class LoanController {
    private final LoanService loanService;
    private final UserRepository userRepo;

    public LoanController(LoanService loanService, UserRepository userRepo) {
        this.loanService = loanService;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<?> getLoans(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of("creditScore", loanService.calculateCreditScore(user), "loans", loanService.getUserLoans(user)));
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(Authentication auth, @RequestBody DTOs.AmountRequest req) {
        return ResponseEntity.ok(loanService.applyForLoan(userRepo.findByUsername(auth.getName()).orElseThrow(), req.amount()));
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<?> repay(Authentication auth, @PathVariable UUID id, @RequestBody DTOs.AmountRequest req) {
        loanService.repayLoan(userRepo.findByUsername(auth.getName()).orElseThrow(), id, req.amount());
        return ResponseEntity.ok(Map.of("message", "Loan repaid"));
    }
}

@RestController @RequestMapping("/api/messages")
class MessagingController {
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;

    public MessagingController(MessageRepository messageRepo, UserRepository userRepo) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/inbox")
    public ResponseEntity<?> inbox(Authentication auth) {
        return ResponseEntity.ok(messageRepo.findByRecipientOrderByCreatedAtDesc(userRepo.findByUsername(auth.getName()).orElseThrow()));
    }

    @PostMapping
    public ResponseEntity<?> send(Authentication auth, @RequestBody DTOs.MessageRequest req) {
        User sender = userRepo.findByUsername(auth.getName()).orElseThrow();
        User recipient = userRepo.findByUsername(req.recipientUsername().toLowerCase().trim()).orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        Message msg = new Message();
        msg.setSender(sender);
        msg.setRecipient(recipient);
        msg.setBody(req.body());
        messageRepo.save(msg);
        return ResponseEntity.ok(Map.of("message", "Message delivered"));
    }
}

@RestController @RequestMapping("/api/networth")
class NetWorthController {
    private final AccountRepository accountRepo;
    private final HoldingRepository holdingRepo;
    private final LoanRepository loanRepo;
    private final UserRepository userRepo;

    public NetWorthController(AccountRepository accountRepo, HoldingRepository holdingRepo, LoanRepository loanRepo, UserRepository userRepo) {
        this.accountRepo = accountRepo;
        this.holdingRepo = holdingRepo;
        this.loanRepo = loanRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<?> getNetWorth(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Account account = accountRepo.findByUser(user).orElseThrow();
        List<Holding> holdings = holdingRepo.findByUser(user);
        List<Loan> activeLoans = loanRepo.findByUserAndStatus(user, "ACTIVE");

        BigDecimal cash = account.getBalance();
        BigDecimal crypto = BigDecimal.ZERO;
        BigDecimal stocks = BigDecimal.ZERO;
        BigDecimal commodities = BigDecimal.ZERO;
        BigDecimal etf = BigDecimal.ZERO;
        BigDecimal realEstate = BigDecimal.ZERO;

        for (Holding h : holdings) {
            BigDecimal val = h.getQuantity().multiply(h.getAsset().getPrice()).setScale(2, RoundingMode.HALF_UP);
            switch (h.getAsset().getCategory().toUpperCase()) {
                case "CRYPTO" -> crypto = crypto.add(val);
                case "STOCKS" -> stocks = stocks.add(val);
                case "COMMODITIES" -> commodities = commodities.add(val);
                case "ETFS" -> etf = etf.add(val);
                case "REALESTATE" -> realEstate = realEstate.add(val);
                default -> commodities = commodities.add(val);
            }
        }

        BigDecimal debt = activeLoans.stream().map(Loan::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netWorth = cash.add(crypto).add(stocks).add(commodities).add(etf).add(realEstate).subtract(debt);

        return ResponseEntity.ok(Map.of(
            "currency", "INR",
            "cash_balance", cash,
            "crypto_value", crypto,
            "stocks_value", stocks,
            "commodities_value", commodities,
            "etf_value", etf,
            "real_estate_value", realEstate,
            "loan_liabilities", debt,
            "net_worth", netWorth
        ));
    }
}

@RestController @RequestMapping("/api/settings")
class SettingsController {
    private final UserRepository userRepo;

    public SettingsController(UserRepository userRepo) { this.userRepo = userRepo; }

    @PatchMapping("/language")
    public ResponseEntity<?> setLanguage(Authentication auth, @RequestBody DTOs.LanguageRequest req) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        user.setPreferredLanguage(req.language());
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "Language preference saved", "language", req.language()));
    }
}
