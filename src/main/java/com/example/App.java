package com.example;

import java.util.ArrayList;
import java.util.List;

public class App {

    // Custom Exceptions
    public static class InvalidInputException extends Exception {
        public InvalidInputException(String message) {
            super(message);
        }
    }

    // Models
    public static class Customer {
        private final String name;
        private final int age;
        private final String idNumber;
        private final double monthlyIncome;
        private final int creditScore;
        private final double existingDebtObligations;

        public Customer(String name, int age, String idNumber, double monthlyIncome, int creditScore, double existingDebtObligations) {
            this.name = name;
            this.age = age;
            this.idNumber = idNumber;
            this.monthlyIncome = monthlyIncome;
            this.creditScore = creditScore;
            this.existingDebtObligations = existingDebtObligations;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getIdNumber() { return idNumber; }
        public double getMonthlyIncome() { return monthlyIncome; }
        public int getCreditScore() { return creditScore; }
        public double getExistingDebtObligations() { return existingDebtObligations; }
    }

    public static class LoanApplication {
        private final double requestedAmount;

        public LoanApplication(double requestedAmount) {
            this.requestedAmount = requestedAmount;
        }

        public double getRequestedAmount() { return requestedAmount; }
    }

    public static class CreditAssessment {
        private final String status; // APPROVED, REJECTED
        private final String riskClassification; // LOW, MEDIUM, HIGH
        private final double maxPermissibleLoan;
        private final List<String> rejectionReasons;

        public CreditAssessment(String status, String riskClassification, double maxPermissibleLoan, List<String> rejectionReasons) {
            this.status = status;
            this.riskClassification = riskClassification;
            this.maxPermissibleLoan = maxPermissibleLoan;
            this.rejectionReasons = rejectionReasons;
        }

        public String getStatus() { return status; }
        public String getRiskClassification() { return riskClassification; }
        public double getMaxPermissibleLoan() { return maxPermissibleLoan; }
        public List<String> getRejectionReasons() { return rejectionReasons; }
    }

    // Business Logic Service
    public static class LoanProcessingService {
        private static final int MIN_AGE = 21;
        private static final double MIN_INCOME = 2000.0;
        private static final int MIN_CREDIT_SCORE = 600;
        private static final double MAX_DTI = 0.45; // 45% maximum DTI limit

        public void validateInput(Customer customer, LoanApplication application) throws InvalidInputException {
            if (customer == null || application == null) {
                throw new InvalidInputException("Customer and Application details cannot be null.");
            }
            if (customer.getName() == null || customer.getName().trim().isEmpty()) {
                throw new InvalidInputException("Customer name cannot be empty.");
            }
            if (customer.getAge() < 0) {
                throw new InvalidInputException("Age cannot be negative.");
            }
            if (customer.getMonthlyIncome() < 0 || customer.getExistingDebtObligations() < 0 || application.getRequestedAmount() <= 0) {
                throw new InvalidInputException("Financial amounts must be positive values.");
            }
            if (customer.getCreditScore() < 300 || customer.getCreditScore() > 850) {
                throw new InvalidInputException("Credit score must be between 300 and 850.");
            }
        }

        public CreditAssessment processApplication(Customer customer, LoanApplication application) throws InvalidInputException {
            validateInput(customer, application);

            List<String> reasons = new ArrayList<>();
            
            // 1. Core Rule Validations
            if (customer.getAge() < MIN_AGE) {
                reasons.add("Customer does not meet the minimum age requirement of " + MIN_AGE + ".");
            }
            if (customer.getIdNumber() == null || customer.getIdNumber().trim().isEmpty()) {
                reasons.add("Invalid or missing government-issued identification number.");
            }
            if (customer.getMonthlyIncome() < MIN_INCOME) {
                reasons.add("Monthly income is below the minimum threshold of " + MIN_INCOME + ".");
            }
            if (customer.getCreditScore() < MIN_CREDIT_SCORE) {
                reasons.add("Credit score is below the minimum requirement of " + MIN_CREDIT_SCORE + ".");
            }

            // 2. DTI Calculation
            double totalPotentialObligation = customer.getExistingDebtObligations();
            double dti = customer.getMonthlyIncome() > 0 ? (totalPotentialObligation / customer.getMonthlyIncome()) : 1.0;
            if (dti > MAX_DTI) {
                reasons.add(String.format("Debt-to-Income ratio (%.2f%%) exceeds the maximum allowed limit of %.2f%%.", (dti * 100), (MAX_DTI * 100)));
            }

            // 3. Max Permissible Loan Calculation (Based on Multiplier rules mapped to Credit Scores)
            double maxLoanMultiplier = 0;
            if (customer.getCreditScore() >= 750) maxLoanMultiplier = 15;
            else if (customer.getCreditScore() >= 650) maxLoanMultiplier = 10;
            else if (customer.getCreditScore() >= MIN_CREDIT_SCORE) maxLoanMultiplier = 5;

            double maxPermissibleLoan = Math.max(0, customer.getMonthlyIncome() * maxLoanMultiplier);

            if (application.getRequestedAmount() > maxPermissibleLoan && maxPermissibleLoan > 0) {
                reasons.add("Requested loan amount exceeds the maximum income-dependent permissible limit of " + maxPermissibleLoan + ".");
            }

            // 4. Decision & Risk Classification Mapping
            String status;
            String riskClassification;

            if (!reasons.isEmpty()) {
                status = "REJECTED";
                riskClassification = "HIGH RISK";
            } else {
                status = "APPROVED";
                // Risk rules
                if (customer.getCreditScore() >= 750 && dti <= 0.20) {
                    riskClassification = "LOW RISK";
                } else {
                    riskClassification = "MEDIUM RISK";
                }
            }

            return new CreditAssessment(status, riskClassification, maxPermissibleLoan, reasons);
        }
    }

    public static void main(String[] args) {
        System.out.println("--- Smart Loan Approval System ---");
        try {
            LoanProcessingService service = new LoanProcessingService();
            Customer customer = new Customer("John Doe", 30, "ID99823", 5000, 780, 500);
            LoanApplication app = new LoanApplication(25000);
            
            CreditAssessment assessment = service.processApplication(customer, app);
            System.out.println("Application Status: " + assessment.getStatus());
            System.out.println("Risk Classification: " + assessment.getRiskClassification());
            System.out.println("Max Permissible Loan: " + assessment.getMaxPermissibleLoan());
        } catch (Exception e) {
            System.err.println("Error processing: " + e.getMessage());
        }
    }
}
