import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Blood Bank Management System - Java AWT
 * Features: Add donor, View all, Search by blood group, Delete donor,
 *           Blood stock, Issue blood. Data is saved in donors.txt
 */
public class BloodBankSystem extends Frame implements ActionListener {

    // ---------- Data ----------
    static class Donor {
        int id; String name; int age; String group; String phone; String city;
        Donor(int id, String name, int age, String group, String phone, String city) {
            this.id = id; this.name = name; this.age = age;
            this.group = group; this.phone = phone; this.city = city;
        }
        String toLine() {
            return id + "," + name + "," + age + "," + group + "," + phone + "," + city;
        }
        String toRow() {
            return String.format("%-4d %-15s %-4d %-6s %-12s %-12s", id, name, age, group, phone, city);
        }
    }

    static final String[] GROUPS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    static final String FILE = "donors.txt";

    ArrayList<Donor> donors = new ArrayList<Donor>();
    Map<String, Integer> stock = new LinkedHashMap<String, Integer>();
    int nextId = 1;

    // ---------- UI components ----------
    TextField tfName, tfAge, tfPhone, tfCity, tfDeleteId;
    Choice chGroup, chSearch, chIssue;
    Button btnAdd, btnView, btnSearch, btnDelete, btnStock, btnIssue, btnClear;
    TextArea output;
    Label status;

    public BloodBankSystem() {
        super("Blood Bank Management System");
        for (String g : GROUPS) stock.put(g, 0);
        loadData();
        buildUI();
        showAll();
    }

    void buildUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));

        // Title
        Label title = new Label("BLOOD BANK MANAGEMENT SYSTEM", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setBackground(new Color(178, 34, 34));
        add(title, BorderLayout.NORTH);

        // Left panel - donor form
        Panel form = new Panel(new GridLayout(0, 2, 6, 10));
        form.add(new Label("Donor Name:"));
        tfName = new TextField(15);
        form.add(tfName);

        form.add(new Label("Age:"));
        tfAge = new TextField(5);
        form.add(tfAge);

        form.add(new Label("Blood Group:"));
        chGroup = new Choice();
        for (String g : GROUPS) chGroup.add(g);
        form.add(chGroup);

        form.add(new Label("Phone:"));
        tfPhone = new TextField(12);
        form.add(tfPhone);

        form.add(new Label("City:"));
        tfCity = new TextField(12);
        form.add(tfCity);

        btnAdd = new Button("Add Donor");
        btnClear = new Button("Clear");
        form.add(btnAdd);
        form.add(btnClear);

        form.add(new Label("Search Group:"));
        chSearch = new Choice();
        for (String g : GROUPS) chSearch.add(g);
        form.add(chSearch);

        btnSearch = new Button("Search");
        btnView = new Button("View All");
        form.add(btnSearch);
        form.add(btnView);

        form.add(new Label("Delete Donor ID:"));
        tfDeleteId = new TextField(5);
        form.add(tfDeleteId);

        btnDelete = new Button("Delete");
        btnStock = new Button("Blood Stock");
        form.add(btnDelete);
        form.add(btnStock);

        form.add(new Label("Issue Blood:"));
        chIssue = new Choice();
        for (String g : GROUPS) chIssue.add(g);
        form.add(chIssue);

        btnIssue = new Button("Issue 1 Unit");
        form.add(btnIssue);
        form.add(new Label(""));

        Panel left = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        left.add(form);
        add(left, BorderLayout.WEST);

        // Center - output area
        output = new TextArea("", 20, 60, TextArea.SCROLLBARS_BOTH);
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(output, BorderLayout.CENTER);

        // Bottom - status
        status = new Label("Welcome!");
        status.setForeground(new Color(0, 100, 0));
        add(status, BorderLayout.SOUTH);

        // Listeners
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        btnSearch.addActionListener(this);
        btnView.addActionListener(this);
        btnDelete.addActionListener(this);
        btnStock.addActionListener(this);
        btnIssue.addActionListener(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveData();
                dispose();
                System.exit(0);
            }
        });

        setSize(1000, 520);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---------- Event handling ----------
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnAdd) addDonor();
        else if (src == btnClear) clearForm();
        else if (src == btnView) showAll();
        else if (src == btnSearch) searchByGroup();
        else if (src == btnDelete) deleteDonor();
        else if (src == btnStock) showStock();
        else if (src == btnIssue) issueBlood();
    }

    void addDonor() {
        String name = tfName.getText().trim();
        String ageStr = tfAge.getText().trim();
        String phone = tfPhone.getText().trim();
        String city = tfCity.getText().trim();

        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty() || city.isEmpty()) {
            setStatus("Please fill in all fields!", true);
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException ex) {
            setStatus("Age must be a number!", true);
            return;
        }
        if (age < 18 || age > 65) {
            setStatus("Donor age must be between 18 and 65!", true);
            return;
        }
        if (!phone.matches("\\d{10}")) {
            setStatus("Phone number must be 10 digits!", true);
            return;
        }
        // A comma in name/city would break the file, so we remove it
        name = name.replace(",", " ");
        city = city.replace(",", " ");

        String group = chGroup.getSelectedItem();
        donors.add(new Donor(nextId++, name, age, group, phone, city));
        stock.put(group, stock.get(group) + 1);
        saveData();
        clearForm();
        showAll();
        setStatus("Donor added successfully! (+1 unit " + group + ")", false);
    }

    void showAll() {
        StringBuilder sb = new StringBuilder();
        sb.append(header());
        for (Donor d : donors) sb.append(d.toRow()).append("\n");
        sb.append("\nTotal donors: ").append(donors.size());
        output.setText(sb.toString());
        setStatus("Displaying all donors", false);
    }

    void searchByGroup() {
        String g = chSearch.getSelectedItem();
        StringBuilder sb = new StringBuilder();
        sb.append("Donors with blood group ").append(g).append("\n\n");
        sb.append(header());
        int count = 0;
        for (Donor d : donors) {
            if (d.group.equals(g)) {
                sb.append(d.toRow()).append("\n");
                count++;
            }
        }
        if (count == 0) sb.append("\nNo donors found for this group.");
        output.setText(sb.toString());
        setStatus(count + " donor(s) found for " + g, count == 0);
    }

    void deleteDonor() {
        int id;
        try {
            id = Integer.parseInt(tfDeleteId.getText().trim());
        } catch (NumberFormatException ex) {
            setStatus("Please enter a valid donor ID!", true);
            return;
        }
        Iterator<Donor> it = donors.iterator();
        while (it.hasNext()) {
            Donor d = it.next();
            if (d.id == id) {
                it.remove();
                int cur = stock.get(d.group);
                if (cur > 0) stock.put(d.group, cur - 1);
                saveData();
                tfDeleteId.setText("");
                showAll();
                setStatus("Donor " + id + " deleted successfully", false);
                return;
            }
        }
        setStatus("No donor found with that ID!", true);
    }

    void showStock() {
        StringBuilder sb = new StringBuilder();
        sb.append("BLOOD STOCK (units available)\n");
        sb.append("------------------------------\n");
        int total = 0;
        for (String g : GROUPS) {
            int u = stock.get(g);
            total += u;
            sb.append(String.format("%-6s : %d%s%n", g, u, u == 0 ? "   (EMPTY)" : ""));
        }
        sb.append("------------------------------\n");
        sb.append("Total units : ").append(total);
        output.setText(sb.toString());
        setStatus("Displaying blood stock", false);
    }

    void issueBlood() {
        String g = chIssue.getSelectedItem();
        int cur = stock.get(g);
        if (cur <= 0) {
            setStatus(g + " is out of stock!", true);
            return;
        }
        stock.put(g, cur - 1);
        saveData();
        showStock();
        setStatus("1 unit " + g + " issued successfully", false);
    }

    // ---------- Helpers ----------
    String header() {
        return String.format("%-4s %-15s %-4s %-6s %-12s %-12s%n", "ID", "Name", "Age", "Group", "Phone", "City")
                + "--------------------------------------------------------\n";
    }

    void clearForm() {
        tfName.setText("");
        tfAge.setText("");
        tfPhone.setText("");
        tfCity.setText("");
        chGroup.select(0);
    }

    void setStatus(String msg, boolean error) {
        status.setForeground(error ? Color.RED : new Color(0, 100, 0));
        status.setText(msg);
    }

    // ---------- File handling ----------
    // Line 1: stock counts; next lines: donors
    void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            StringBuilder s = new StringBuilder("STOCK");
            for (String g : GROUPS) s.append(",").append(stock.get(g));
            pw.println(s);
            for (Donor d : donors) pw.println(d.toLine());
        } catch (IOException ex) {
            System.out.println("Save error: " + ex.getMessage());
        }
    }

    void loadData() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p[0].equals("STOCK")) {
                    for (int i = 0; i < GROUPS.length && i + 1 < p.length; i++)
                        stock.put(GROUPS[i], Integer.parseInt(p[i + 1]));
                } else if (p.length == 6) {
                    int id = Integer.parseInt(p[0]);
                    donors.add(new Donor(id, p[1], Integer.parseInt(p[2]), p[3], p[4], p[5]));
                    if (id >= nextId) nextId = id + 1;
                }
            }
        } catch (Exception ex) {
            System.out.println("Load error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        new BloodBankSystem();
    }
}

