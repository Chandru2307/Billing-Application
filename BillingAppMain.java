import java.util.*;

// ------------------ PLAN CLASSES -------------------
class Plan {
    protected int planId;
    protected String name;
    protected double monthlyPrice;
    protected List<String> features;
    protected int trialDays;

    public Plan(int planId, String name, double monthlyPrice, List<String> features, int trialDays) {
        this.planId = planId;
        this.name = name;
        this.monthlyPrice = monthlyPrice;
        this.features = features;
        this.trialDays = trialDays;
    }

    public int getPlanId() { return planId; }
    public String getName() { return name; }
    public double getMonthlyPrice() { return monthlyPrice; }

    // Overridable method
    public double computeAmount() {
        return monthlyPrice;
    }

    public String toString() {
        return planId + ". " + name + " (₹" + monthlyPrice + "/month)";
    }
}

class MonthlyPlan extends Plan {
    public MonthlyPlan(int planId, String name, double monthlyPrice, List<String> features, int trialDays) {
        super(planId, name, monthlyPrice, features, trialDays);
    }
    @Override
    public double computeAmount() {
        return monthlyPrice;
    }
}

class AnnualPlan extends Plan {
    public AnnualPlan(int planId, String name, double monthlyPrice, List<String> features, int trialDays) {
        super(planId, name, monthlyPrice, features, trialDays);
    }
    @Override
    public double computeAmount() {
        return monthlyPrice * 12 * 0.9; // 10% discount
    }
}

// ------------------ SUBSCRIBER -------------------
class Subscriber {
    private int id;
    private String name;
    private String email;
    private Plan currentPlan;
    private String status; // Active, Cancelled

    public Subscriber(int id, String name, String email, Plan currentPlan) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.currentPlan = currentPlan;
        this.status = "Active";
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Plan getCurrentPlan() { return currentPlan; }
    public String getStatus() { return status; }

    public void changePlan(Plan newPlan) {
        this.currentPlan = newPlan;
        System.out.println(name + " changed to " + newPlan.getName());
    }

    public void cancelSubscription() {
        this.status = "Cancelled";
        System.out.println(name + "'s subscription cancelled.");
    }
}

// ------------------ INVOICE -------------------
class Invoice {
    private static int invoiceCounter = 1000;
    private int invoiceNo;
    private int subscriberId;
    private double amount;
    private Date dueDate;
    private String state; // Pending, Paid, Overdue

    public Invoice(int subscriberId, double amount, Date dueDate) {
        this.invoiceNo = invoiceCounter++;
        this.subscriberId = subscriberId;
        this.amount = amount;
        this.dueDate = dueDate;
        this.state = "Pending";
    }

    public int getInvoiceNo() { return invoiceNo; }
    public int getSubscriberId() { return subscriberId; }
    public double getAmount() { return amount; }
    public String getState() { return state; }

    public void markPaid() { state = "Paid"; }
    public void markOverdue() {
        if (!state.equals("Paid")) state = "Overdue";
    }

    public String toString() {
        return "Invoice#" + invoiceNo + " | Subscriber: " + subscriberId +
               " | Amount: ₹" + amount + " | State: " + state;
    }
}

// ------------------ BILLING SERVICE -------------------
class BillingService {
    private List<Invoice> invoices = new ArrayList<>();
    private double totalRevenue = 0.0;

    public Invoice generateInvoice(Subscriber sub) {
        double amt = sub.getCurrentPlan().computeAmount();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 15);
        Invoice inv = new Invoice(sub.getId(), amt, cal.getTime());
        invoices.add(inv);
        System.out.println("Generated " + inv);
        return inv;
    }

    public void recordPayment(int invoiceNo) {
        for (Invoice inv : invoices) {
            if (inv.getInvoiceNo() == invoiceNo) {
                if (!inv.getState().equals("Paid")) {
                    inv.markPaid();
                    totalRevenue += inv.getAmount();
                    System.out.println("Payment done for Invoice#" + invoiceNo);
                }
                return;
            }
        }
        System.out.println("Invoice not found!");
    }

    public void showInvoices() {
        for (Invoice inv : invoices) {
            System.out.println(inv);
        }
    }

    public void showRevenueReport() {
        System.out.println("Total Revenue: ₹" + totalRevenue);
    }
}

// ------------------ MAIN -------------------
public class BillingAppMain {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Plans
        Plan monthly = new MonthlyPlan(1, "Basic Monthly", 500, Arrays.asList("F1","F2"), 7);
        Plan annual = new AnnualPlan(2, "Premium Annual", 450, Arrays.asList("F1","F2","F3"), 14);

        List<Subscriber> subscribers = new ArrayList<>();
        BillingService billing = new BillingService();

        while (true) {
            System.out.println("\n==== MENU ====");
            System.out.println("1. Add Subscriber");
            System.out.println("2. Change Plan");
            System.out.println("3. Cancel Subscription");
            System.out.println("4. Generate Invoice");
            System.out.println("5. Record Payment");
            System.out.println("6. Show All Invoices");
            System.out.println("7. Show Revenue Report");
            System.out.println("0. Exit");
            System.out.print("Choose option: ");
            int choice = sc.nextInt(); sc.nextLine();

            switch (choice) {
                case 1: // Add
                    System.out.print("Enter ID: ");
                    int id = sc.nextInt(); sc.nextLine();
                    System.out.print("Enter Name: ");
                    String name = sc.nextLine();
                    System.out.print("Enter Email: ");
                    String email = sc.nextLine();
                    System.out.println("Choose Plan: 1.Monthly  2.Annual");
                    int p = sc.nextInt(); sc.nextLine();
                    Plan selected = (p==1)? monthly : annual;
                    subscribers.add(new Subscriber(id, name, email, selected));
                    System.out.println("Subscriber Added!");
                    break;
                case 2: // Change Plan
                    System.out.print("Enter Subscriber ID: ");
                    int sid = sc.nextInt(); sc.nextLine();
                    for (Subscriber sub : subscribers) {
                        if (sub.getId()==sid) {
                            System.out.println("Choose Plan: 1.Monthly 2.Annual");
                            int pp = sc.nextInt(); sc.nextLine();
                            sub.changePlan((pp==1)? monthly:annual);
                        }
                    }
                    break;
                case 3: // Cancel
                    System.out.print("Enter Subscriber ID: ");
                    int csid = sc.nextInt(); sc.nextLine();
                    for (Subscriber sub : subscribers) {
                        if (sub.getId()==csid) sub.cancelSubscription();
                    }
                    break;
                case 4: // Generate Invoice
                    System.out.print("Enter Subscriber ID: ");
                    int gi = sc.nextInt(); sc.nextLine();
                    for (Subscriber sub : subscribers) {
                        if (sub.getId()==gi) billing.generateInvoice(sub);
                    }
                    break;
                case 5: // Record Payment
                    System.out.print("Enter Invoice No: ");
                    int ino = sc.nextInt(); sc.nextLine();
                    billing.recordPayment(ino);
                    break;
                case 6:
                    billing.showInvoices();
                    break;
                case 7:
                    billing.showRevenueReport();
                    break;
                case 0:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
}