# Payment Processing System

## Overview
This project defines a payment processing system that supports multiple payment channels, prevents duplicate payments, validates transactions, handles failures reliably, and provides a simple shared interface for single user.

## Meeting Notes for 30-07-2026
Multiple payment channels need to be supported, including UPI, Card Payments, and NetBanking.

To avoid duplicate payments, the system should check whether the user is attempting to pay for the same invoice more than once.

Various validation rules were discussed to ensure secure and error‑free transactions.

A clear solution for handling payment failures is required, including user notifications and retry options.

The system will have a single user interface, but it must support multiple end users simultaneously.

The interface should be simple, intuitive, and ready to use, ensuring a smooth user experience.