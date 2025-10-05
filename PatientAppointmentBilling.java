/*
 UML (textual brief):
 Person <-- Patient
 Person <-- Doctor

 Appointment --aggregates--> Patient
 Appointment --aggregates--> Doctor
 Appointment "1" <--> "0..1" Consultation
 Consultation --aggregates--> Prescription
 Consultation --associates--> Invoice
 Invoice --aggregates--> Prescription Items
 Invoice --composed-with--> Payment

*/

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PatientAppointmentBilling {
    // --- Entry point ---
    public static void main(String[] args) {
        ConsoleApp app = new ConsoleApp();
        app.run();
    }

    // -------------------- Core app --------------------
    static class ConsoleApp {
        private final Scanner sc = new Scanner(System.in);
        private final Clinic clinic = new Clinic();
        private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        void run() {
            seedSampleData();
            boolean running = true;
            while (running) {
                printMenu();
                String choice = sc.nextLine().trim();
                switch (choice) {
                    case "1": addPatient(); break;
                    case "2": addDoctor(); break;
                    case "3": scheduleAppointment(); break;
                    case "4": recordConsultation(); break;
                    case "5": generateInvoice(); break;
                    case "6": recordPayment(); break;
                    case "7": listAppointments(); break;
                    case "8": clinic.reportOutstanding(); break;
                    case "9": running = false; System.out.println("Exiting..."); break;
                    default: System.out.println("Invalid choice");
                }
            }
        }

        void printMenu() {
            System.out.println("\n--- Patient Appointment & Billing ---");
            System.out.println("1. Add Patient");
            System.out.println("2. Add Doctor");
            System.out.println("3. Schedule Appointment");
            System.out.println("4. Record Consultation");
            System.out.println("5. Generate Invoice");
            System.out.println("6. Record Payment");
            System.out.println("7. List Appointments");
            System.out.println("8. Outstanding Dues Report");
            System.out.println("9. Exit");
            System.out.print("Choice: ");
        }

        void addPatient() {
            System.out.print("Patient name: ");
            String name = sc.nextLine();
            System.out.print("Contact number: ");
            String contact = sc.nextLine();
            Patient p = clinic.addPatient(name, contact);
            System.out.println("Added Patient: " + p);
        }

        void addDoctor() {
            System.out.print("Doctor name: ");
            String name = sc.nextLine();
            System.out.print("Specialty: ");
            String spec = sc.nextLine();
            Doctor d = clinic.addDoctor(name, spec);
            System.out.println("Added Doctor: " + d);
        }

        void scheduleAppointment() {
            System.out.print("Patient ID: ");
            int pid = Integer.parseInt(sc.nextLine());
            System.out.print("Doctor ID: ");
            int did = Integer.parseInt(sc.nextLine());
            System.out.print("Appointment date/time (yyyy-MM-dd HH:mm): ");
            LocalDateTime dt;
            try {
                dt = LocalDateTime.parse(sc.nextLine(), dtf);
            } catch (Exception e) { System.out.println("Bad date format"); return; }
            try {
                Appointment a = clinic.scheduleAppointment(pid, did, dt);
                System.out.println("Appointment scheduled: " + a.confirmationString());
            } catch (IllegalArgumentException ex) {
                System.out.println("Could not schedule: " + ex.getMessage());
            }
        }

        void recordConsultation() {
            System.out.print("Appointment ID: ");
            int aid = Integer.parseInt(sc.nextLine());
            Appointment ap = clinic.findAppointmentById(aid);
            if (ap == null) { System.out.println("Appointment not found"); return; }
            if (ap.getStatus() != AppointmentStatus.COMPLETED) {
                System.out.println("Consultation can only be recorded for completed appointments. Current status: " + ap.getStatus());
                return;
            }
            System.out.print("Consultation notes: ");
            String notes = sc.nextLine();
            System.out.print("Consultation fee: ");
            double fee = Double.parseDouble(sc.nextLine());
            Consultation c = clinic.recordConsultation(ap.getId(), notes, fee);
            System.out.println("Recorded consultation:\n" + c.summaryString());
            System.out.println("Add prescriptions now? (y/n)");
            String yn = sc.nextLine().trim();
            if (yn.equalsIgnoreCase("y")) {
                while (true) {
                    System.out.print("Item name (or blank to finish): ");
                    String item = sc.nextLine();
                    if (item.isEmpty()) break;
                    System.out.print("Quantity: ");
                    int q = Integer.parseInt(sc.nextLine());
                    System.out.print("Unit price: ");
                    double up = Double.parseDouble(sc.nextLine());
                    c.getPrescription().addItem(new PrescriptionItem(item, q, up));
                }
                System.out.println("Prescription recorded.");
            }
        }

        void generateInvoice() {
            System.out.print("Consultation ID: ");
            int cid = Integer.parseInt(sc.nextLine());
            try {
                Invoice inv = clinic.generateInvoice(cid);
                System.out.println(inv.toString());
            } catch (IllegalArgumentException ex) { System.out.println("Cannot create invoice: " + ex.getMessage()); }
        }

        void recordPayment() {
            System.out.print("Invoice ID: ");
            int iid = Integer.parseInt(sc.nextLine());
            Invoice inv = clinic.findInvoiceById(iid);
            if (inv == null) { System.out.println("Invoice not found"); return; }
            if (inv.getStatus() == InvoiceStatus.CLOSED) { System.out.println("Invoice already closed"); return; }
            System.out.print("Payment type (cash/card): ");
            String type = sc.nextLine().trim();
            Payment p;
            if (type.equalsIgnoreCase("cash")) {
                System.out.print("Amount: "); double amt = Double.parseDouble(sc.nextLine());
                p = new CashPayment(amt);
            } else {
                System.out.print("Card holder name: "); String name = sc.nextLine();
                System.out.print("Amount: "); double amt = Double.parseDouble(sc.nextLine());
                p = new CardPayment(amt, name);
            }
            try {
                clinic.recordPayment(iid, p);
                System.out.println("Payment recorded. Receipt:\n" + p.receiptString(iid, inv.getPatient()));
            } catch (IllegalArgumentException ex) { System.out.println("Payment failed: " + ex.getMessage()); }
        }

        void listAppointments() {
            System.out.print("Filter by doctor id (blank for all): ");
            String t = sc.nextLine().trim();
            Integer did = t.isEmpty() ? null : Integer.parseInt(t);
            clinic.listAppointments(did).forEach(a -> System.out.println(a));
        }

        void seedSampleData() {
            Patient p1 = clinic.addPatient("Ramesh", "9876543210");
            Patient p2 = clinic.addPatient("Sita", "9123456780");
            Doctor d1 = clinic.addDoctor("Dr. Anand", "General");
            Doctor d2 = clinic.addDoctor("Dr. Kavya", "ENT");
            // Pre-existing appointment - to demonstrate completed status
            try {
                Appointment a = clinic.scheduleAppointment(p1.getId(), d1.getId(), LocalDateTime.now().minusDays(1).withHour(10).withMinute(0));
                a.setStatus(AppointmentStatus.COMPLETED);
            } catch (Exception ignored) {}
        }
    }

    // -------------------- Domain / Model --------------------
    static abstract class Person {
        protected final int id;
        protected String name;
        protected String contact;

        Person(int id, String name, String contact) {
            this.id = id; this.name = name; this.contact = contact;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getContact() { return contact; }

        @Override
        public String toString() { return String.format("%s[id=%d,name=%s,contact=%s]", this.getClass().getSimpleName(), id, name, contact); }
    }

    static class Patient extends Person {
        Patient(int id, String name, String contact) { super(id, name, contact); }
    }

    static class Doctor extends Person {
        private String specialty;

        Doctor(int id, String name, String contact) { super(id, name, contact); }
        Doctor(int id, String name, String specialty, String contact) { super(id, name, contact); this.specialty = specialty; }

        public String getSpecialty() { return specialty; }

        @Override
        public String toString() { return String.format("Doctor[id=%d,name=%s,specialty=%s]", id, name, specialty); }
    }

    enum AppointmentStatus { SCHEDULED, COMPLETED, CANCELLED }

    static class Appointment {
        private static final AtomicInteger seq = new AtomicInteger(1);
        private final int id;
        private final Patient patient;
        private final Doctor doctor;
        private final LocalDateTime slot;
        private AppointmentStatus status = AppointmentStatus.SCHEDULED;

        Appointment(Patient patient, Doctor doctor, LocalDateTime slot) {
            this.id = seq.getAndIncrement();
            this.patient = patient; this.doctor = doctor; this.slot = slot;
        }

        public int getId() { return id; }
        public Patient getPatient() { return patient; }
        public Doctor getDoctor() { return doctor; }
        public LocalDateTime getSlot() { return slot; }
        public AppointmentStatus getStatus() { return status; }
        public void setStatus(AppointmentStatus s) { this.status = s; }

        public String confirmationString() {
            return String.format("Appointment[id=%d, patient=%s, doctor=%s, at=%s, status=%s]",
                    id, patient.getName(), doctor.getName(), slot.toString(), status);
        }

        @Override
        public String toString() {
            return confirmationString();
        }
    }

    static class PrescriptionItem {
        private final String name; private final int qty; private final double unitPrice;
        PrescriptionItem(String name, int qty, double unitPrice) { this.name = name; this.qty = qty; this.unitPrice = unitPrice; }
        public double total() { return qty * unitPrice; }
        @Override public String toString() { return String.format("%s x%d @ %.2f => %.2f", name, qty, unitPrice, total()); }
    }

    static class Prescription {
        private final List<PrescriptionItem> items = new ArrayList<>();
        public void addItem(PrescriptionItem it) { items.add(it); }
        public List<PrescriptionItem> getItems() { return Collections.unmodifiableList(items); }
        public double itemsTotal() { return items.stream().mapToDouble(PrescriptionItem::total).sum(); }
        @Override public String toString() { if (items.isEmpty()) return "(no items)"; StringBuilder sb=new StringBuilder(); for (PrescriptionItem i: items) sb.append(i).append("\n"); return sb.toString(); }
    }

    static class Consultation {
        private static final AtomicInteger seq = new AtomicInteger(1);
        private final int id;
        private final Appointment appointment;
        private final Patient patient;
        private final Doctor doctor;
        private final String notes;
        private final double fee;
        private final Prescription prescription = new Prescription();
        private Invoice invoice; // linked invoice

        Consultation(Appointment appointment, String notes, double fee) {
            this.id = seq.getAndIncrement();
            this.appointment = appointment;
            this.patient = appointment.getPatient();
            this.doctor = appointment.getDoctor();
            this.notes = notes; this.fee = fee;
        }

        public int getId() { return id; }
        public Appointment getAppointment() { return appointment; }
        public Patient getPatient() { return patient; }
        public Doctor getDoctor() { return doctor; }
        public Prescription getPrescription() { return prescription; }
        public double getFee() { return fee; }
        public void linkInvoice(Invoice inv) { this.invoice = inv; }
        public Invoice getInvoice() { return invoice; }

        public String summaryString() {
            return String.format("Consultation[id=%d, appointment=%d, patient=%s, doctor=%s, fee=%.2f, notes=%s]\nPrescription:\n%s",
                    id, appointment.getId(), patient.getName(), doctor.getName(), fee, notes, prescription.toString());
        }
    }

    enum InvoiceStatus { OPEN, CLOSED }

    static class Invoice {
        private static final AtomicInteger seq = new AtomicInteger(1);
        private final int id;
        private final Consultation consultation;
        private final Patient patient;
        private final List<PrescriptionItem> items;
        private final double consultationFee;
        private final double taxRate = 0.12; // 12% tax
        private InvoiceStatus status = InvoiceStatus.OPEN;

        Invoice(Consultation consultation) {
            this.id = seq.getAndIncrement();
            this.consultation = consultation;
            this.patient = consultation.getPatient();
            this.items = new ArrayList<>(consultation.getPrescription().getItems());
            this.consultationFee = consultation.getFee();
        }

        public int getId() { return id; }
        public Patient getPatient() { return patient; }
        public InvoiceStatus getStatus() { return status; }
        public void close() { this.status = InvoiceStatus.CLOSED; }

        public double itemsTotal() { return items.stream().mapToDouble(PrescriptionItem::total).sum(); }
        public double subtotal() { return consultationFee + itemsTotal(); }
        public double tax() { return subtotal() * taxRate; }
        public double total() { return subtotal() + tax(); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Invoice[id=%d, patient=%s, status=%s]\n", id, patient.getName(), status));
            sb.append(String.format("Consultation fee: %.2f\n", consultationFee));
            sb.append("Items:\n");
            if (items.isEmpty()) sb.append("(none)\n"); else items.forEach(it -> sb.append("  ").append(it).append("\n"));
            sb.append(String.format("Subtotal: %.2f\nTax(%.0f%%): %.2f\nTOTAL: %.2f\n", subtotal(), taxRate*100, tax(), total()));
            return sb.toString();
        }
    }

    enum PaymentStatus { PENDING, SETTLED }

    static abstract class Payment {
        protected final double amount;
        protected final LocalDateTime date = LocalDateTime.now();
        protected PaymentStatus status = PaymentStatus.PENDING;

        Payment(double amount) { this.amount = amount; }
        public double getAmount() { return amount; }
        public void settle() { this.status = PaymentStatus.SETTLED; }
        public PaymentStatus getStatus() { return status; }

        public abstract String receiptString(int invoiceId, Patient patient);
    }

    static class CashPayment extends Payment {
        CashPayment(double amount) { super(amount); }
        @Override public String receiptString(int invoiceId, Patient patient) {
            return String.format("Receipt - Cash\nInvoice: %d\nPatient: %s\nAmount: %.2f\nDate: %s\n", invoiceId, patient.getName(), amount, date.toString());
        }
    }

    static class CardPayment extends Payment {
        private final String cardHolder;
        CardPayment(double amount, String cardHolder) { super(amount); this.cardHolder = cardHolder; }
        @Override public String receiptString(int invoiceId, Patient patient) {
            return String.format("Receipt - Card\nInvoice: %d\nPatient: %s\nCard Holder: %s\nAmount: %.2f\nDate: %s\n", invoiceId, patient.getName(), cardHolder, amount, date.toString());
        }
    }

    // -------------------- Clinic service (manages lists and business rules) --------------------
    static class Clinic {
        private final Map<Integer, Patient> patients = new HashMap<>();
        private final Map<Integer, Doctor> doctors = new HashMap<>();
        private final Map<Integer, Appointment> appointments = new HashMap<>();
        private final Map<Integer, Consultation> consultations = new HashMap<>();
        private final Map<Integer, Invoice> invoices = new HashMap<>();
        private final Map<Integer, Payment> payments = new HashMap<>();

        private final AtomicInteger patientSeq = new AtomicInteger(1);
        private final AtomicInteger doctorSeq = new AtomicInteger(1);

        public Patient addPatient(String name, String contact) {
            int id = patientSeq.getAndIncrement();
            Patient p = new Patient(id, name, contact);
            patients.put(id, p);
            return p;
        }

        public Doctor addDoctor(String name, String specialty) {
            int id = doctorSeq.getAndIncrement();
            Doctor d = new Doctor(id, name, specialty, "");
            doctors.put(id, d);
            return d;
        }

        public Appointment scheduleAppointment(int patientId, int doctorId, LocalDateTime slot) {
            Patient p = patients.get(patientId);
            Doctor d = doctors.get(doctorId);
            if (p == null) throw new IllegalArgumentException("Patient not found");
            if (d == null) throw new IllegalArgumentException("Doctor not found");
            // Business rule: appointments can be scheduled only in available slots -> check doctor has no appointment at this slot
            for (Appointment a : appointments.values()) {
                if (a.getDoctor().getId() == doctorId && a.getSlot().equals(slot) && a.getStatus() == AppointmentStatus.SCHEDULED) {
                    throw new IllegalArgumentException("Doctor already has appointment at that slot");
                }
            }
            Appointment ap = new Appointment(p, d, slot);
            appointments.put(ap.getId(), ap);
            return ap;
        }

        public Appointment findAppointmentById(int id) { return appointments.get(id); }

        public List<Appointment> listAppointments(Integer doctorId) {
            List<Appointment> out = new ArrayList<>();
            for (Appointment a: appointments.values()) {
                if (doctorId == null || a.getDoctor().getId() == doctorId) out.add(a);
            }
            out.sort(Comparator.comparing(Appointment::getSlot));
            return out;
        }

        public Consultation recordConsultation(int appointmentId, String notes, double fee) {
            Appointment a = appointments.get(appointmentId);
            if (a == null) throw new IllegalArgumentException("Appointment not found");
            if (a.getStatus() != AppointmentStatus.COMPLETED) throw new IllegalArgumentException("Appointment must be completed before recording consultation");
            Consultation c = new Consultation(a, notes, fee);
            consultations.put(c.getId(), c);
            return c;
        }

        public Invoice generateInvoice(int consultationId) {
            Consultation c = consultations.get(consultationId);
            if (c == null) throw new IllegalArgumentException("Consultation not found");
            if (c.getInvoice() != null) throw new IllegalArgumentException("Invoice already generated for this consultation");
            Invoice inv = new Invoice(c);
            invoices.put(inv.getId(), inv);
            c.linkInvoice(inv);
            return inv;
        }

        public Invoice findInvoiceById(int id) { return invoices.get(id); }

        public void recordPayment(int invoiceId, Payment payment) {
            Invoice inv = invoices.get(invoiceId);
            if (inv == null) throw new IllegalArgumentException("Invoice not found");
            double due = inv.total();
            if (payment.getAmount() < due - 0.0001) throw new IllegalArgumentException("Payment amount is less than invoice total. Partial payments not supported in this simple model.");
            // Accept payment
            payment.settle();
            payments.put(payment.hashCode(), payment);
            inv.close();
        }

        public Consultation findConsultationById(int id) { return consultations.get(id); }

        public void reportOutstanding() {
            System.out.println("\n--- Outstanding Invoices ---");
            invoices.values().stream().filter(inv -> inv.getStatus() == InvoiceStatus.OPEN).forEach(inv -> System.out.println(inv));
        }
    }
}
