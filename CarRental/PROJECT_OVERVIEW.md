# CarRental Project Overview

## Application Entry Point
- `CarRentalApplication` bootstraps the Spring Boot application via `SpringApplication.run` and lives under the `CarRental.example` package.

## Domain Models
- `User` documents capture credentials, role, enablement flags, uploaded license/ID card binaries, verification states, and audit timestamps for MongoDB storage.
- `Vehicle` documents describe fleet metadata such as plate, type, brand, battery level, pricing, assigned station, availability, booking status, and any maintenance issues.

## Core Services
- `VehicleService` wraps `VehicleRepository` to update availability, place or release pending-payment holds, and safely mark a vehicle as rented without overwriting another hold.
- `RentalRecordService` aggregates rental history and statistics, orchestrates status transitions (sign contract, check-in, request return), builds admin dashboards (revenue/peak hours), and surfaces AI-style operational suggestions based on station demand and car-type popularity.

## Authentication Flow
- `AuthController` renders the register page and validates submitted registrations: it guards against duplicate usernames, enforces password confirmation, applies a default `USER` role, encodes the password, and saves the user before redirecting to the login page.

## Rental Booking Lifecycle
- `RentalController` drives the renter experience:
  - `checkout` fetches vehicle pricing info for a selected station.
  - `bookRental` validates availability, parses requested dates, creates a `RentalRecord` with a five-minute payment hold, and marks the vehicle as pending payment.
  - `confirmPayment` recalculates totals, enforces document uploads for bank transfers, and keeps the hold active while recording the payment method.
  - `getRental` exposes booking details (vehicle, station, status) while expiring unpaid holds when necessary.
  - `history` and `stats` delegate to `RentalRecordService` for user-facing history and spending analytics.
  - `cancelRental` clears holds and marks rentals cancelled; `signContract`, `checkIn`, and `requestReturn` advance the workflow toward on-trip and return states while syncing vehicle availability.
  - `getAllHistoryForAdmin` provides administrators with full rental data.
