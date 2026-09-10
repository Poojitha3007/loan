package com.example;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.example.App.*;
import java.util.List;

public class AppTest {

    private LoanProcessingService service;

    @Before
    public void setUp() {
        service = new LoanProcessingService();
    }

    // --- POSITIVE & OPTIMAL CASE TESTS ---

    @Test
    public void testLowRiskApprovedApplication() throws InvalidInputException {
        Customer customer = new Customer("Alice Smith", 28, "ID12345", 6000.0, 800, 600.0); // DTI = 10%
        LoanApplication app = new LoanApplication(30000.0); // Limit is 6000 * 15 = 90000

        CreditAssessment assessment = service.processApplication(customer, app);

        assertEquals("APPROVED", assessment.getStatus());
        assertEquals("LOW RISK", assessment.getRiskClassification());
        assertEquals(90000.0, assessment.getMaxPermissibleLoan(), 0.001);
        assertTrue(assessment.getRejectionReasons().isEmpty());
    }

    @Test
    public void testMediumRiskApprovedApplication() throws InvalidInputException {
        Customer customer = new Customer("Bob Jones", 35, "ID67890", 4000.0, 680, 1000.0); // DTI = 25% (Medium Risk)
        LoanApplication app = new LoanApplication(20000.0); // Limit is 4000 * 10 = 40000

        CreditAssessment assessment = service.processApplication(customer, app);

        assertEquals("APPROVED", assessment.getStatus());
        assertEquals("MEDIUM RISK", assessment.getRiskClassification());
        assertTrue(assessment.getRejectionReasons().isEmpty());
    }

    // --- BOUNDARY CASE TESTS ---

    @Test
    public void testExactBoundaryLimits() throws InvalidInputException {
        // Testing exact boundary parameters: Age=21, Credit=600, DTI=45% (45% of 2000 is 900)
        Customer customer = new Customer("Edge Case", 21, "ID21600", 2000.0, 600, 900.0);
        LoanApplication app = new LoanApplication(10000.0); // Max Limit = 2000 * 5 = 10000

        CreditAssessment assessment = service.processApplication(customer, app);

        assertEquals("APPROVED", assessment.getStatus());
        assertEquals("MEDIUM RISK", assessment.getRiskClassification());
        assertEquals(10000.0, assessment.getMaxPermissibleLoan(), 0.001);
        assertTrue(assessment.getRejectionReasons().isEmpty());
    }

    // --- NEGATIVE CASE TESTS (REJECTIONS) ---

    @Test
    public void testMultipleRejectionReasons() throws InvalidInputException {
        // Violates multiple core metrics: Underage (20), Missing ID, and Poor Credit (550)
        Customer customer = new Customer("Underage Fail", 20, "", 5000.0, 550, 200.0);
        LoanApplication app = new LoanApplication(10000.0);

        CreditAssessment assessment = service.processApplication(customer, app);

        assertEquals("REJECTED", assessment.getStatus());
        assertEquals("HIGH RISK", assessment.getRiskClassification());
        
        List<String> reasons = assessment.getRejectionReasons();
        assertTrue(reasons.stream().anyMatch(r -> r.contains("minimum age")));
        assertTrue(reasons.stream().anyMatch(r -> r.contains("identification number")));
        assertTrue(reasons.stream().anyMatch(r -> r.contains("Credit score is below")));
    }

    @Test
    public void testExceededLoanAmountRejection() throws InvalidInputException {
        Customer customer = new Customer("Charlie Brown", 30, "ID5544", 3000.0, 700, 300.0);
        // Max permissible limit = 3000 * 10 = 30000
        LoanApplication app = new LoanApplication(35000.0); 

        CreditAssessment assessment = service.processApplication(customer, app);

        assertEquals("REJECTED", assessment.getStatus());
        List<String> reasons = assessment.getRejectionReasons();
        assertTrue(reasons.stream().anyMatch(r -> r.contains("exceeds the maximum income-dependent permissible limit")));
    }

    @Test
    public void testExceededDTILimitRejection() throws InvalidInputException {
        Customer customer = new Customer("Debt Heavy", 40, "ID9922", 4000.0, 720, 2000.0); // DTI = 50% (>45%)
        LoanApplication app = new LoanApplication(10000.0);

        CreditAssessment assessment = service.processApplication(customer, app);

        assertEquals("REJECTED", assessment.getStatus());
        assertTrue(assessment.getRejectionReasons().stream().anyMatch(r -> r.contains("Debt-to-Income ratio")));
    }

    // --- INPUT VALIDATION & EXCEPTION TESTS ---

    @Test(expected = InvalidInputException.class)
    public void testInvalidCreditScoreThrowsException() throws InvalidInputException {
        Customer customer = new Customer("Invalid Credit", 30, "ID111", 4000.0, 900, 200.0); // Out of bounds max limit (850)
        LoanApplication app = new LoanApplication(5000.0);
        service.processApplication(customer, app);
    }

    @Test(expected = InvalidInputException.class)
    public void testNegativeFinancialAmountsThrowException() throws InvalidInputException {
        Customer customer = new Customer("Negative Income", 30, "ID222", -100.0, 700, 200.0);
        LoanApplication app = new LoanApplication(5000.0);
        service.processApplication(customer, app);
    }
}
