// full file: Pl.java
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.swing.*;
import javax.swing.JFormattedTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;

/* ------------------------------------------------------------
   Placement Management System (3-column layout, popup outputs)
   - First column: Student + Company actions (stacked)
   - Second column: Skills
   - Third column: Drive & Reports
   - Output is shown in a popup dialog automatically when an action returns results
   ------------------------------------------------------------ */

/* Custom Exceptions */
class InvalidDataException extends Exception {
    public InvalidDataException(String msg) { super(msg); }
}

class RecordNotFoundException extends Exception {
    public RecordNotFoundException(String msg) { super(msg); }
}

/* Base class Person */
abstract class Person implements Serializable {
    protected String id;
    protected String name;

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return "ID: " + id + " | Name: " + name;
    }
}

/* Skill */
class Skill implements Serializable {
    private String name, level;
    public Skill(String name, String level) { this.name = name; this.level = level; }
    public String getName() { return name; }
    public String getLevel() { return level; }
    @Override public String toString() { return name + "(" + level + ")"; }
}

/* Student (uses float cgpa) */
class Student extends Person {
    private String branch;
    private float cgpa;
    private List<Skill> skills = new ArrayList<>();

    public Student(String id, String name, String branch, float cgpa) {
        super(id, name);
        this.branch = branch;
        this.cgpa = cgpa;
    }

    public void addSkill(Skill s) { skills.add(s); }
    public List<Skill> getSkills() { return skills; }
    public String getBranch() { return branch; }
    public float getCgpa() { return cgpa; }

    @Override
    public String toString() {
        return super.toString() + " | Branch: " + branch + " | CGPA: " + String.format("%.2f", cgpa) + " | Skills: " + skills;
    }
}

/* Company (minCgpa as float) */
class Company implements Serializable {
    private String id, name, role;
    private float minCgpa;
    private List<Skill> requiredSkills = new ArrayList<>();

    public Company(String id, String name, String role, float minCgpa) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.minCgpa = minCgpa;
    }

    public void addRequiredSkill(Skill s) { requiredSkills.add(s); }
    public List<Skill> getRequiredSkills() { return requiredSkills; }
    public float getMinCgpa() { return minCgpa; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }

    @Override
    public String toString() {
        return "Company ID: " + id + " | Name: " + name + " | Role: " + role + " | MinCGPA: " + String.format("%.2f", minCgpa) + " | RequiredSkills: " + requiredSkills;
    }
}

/* Placement record */
class PlacementRecord implements Serializable {
    private String studentId, companyId, status;
    private Date date;

    public PlacementRecord(String studentId, String companyId, String status) {
        this.studentId = studentId;
        this.companyId = companyId;
        this.status = status;
        this.date = new Date();
    }

    @Override
    public String toString() {
        return "Student: " + studentId + " | Company: " + companyId + " | Status: " + status + " | Date: " + date;
    }
}

/* NumericDocumentFilter: allows only digits and single dot while typing */
class NumericDocumentFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (string == null) return;
        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
        StringBuilder sb = new StringBuilder(current);
        sb.insert(offset, string);
        if (isAcceptable(sb.toString())) super.insertString(fb, offset, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
        StringBuilder sb = new StringBuilder(current);
        sb.replace(offset, offset + length, text == null ? "" : text);
        if (isAcceptable(sb.toString())) super.replace(fb, offset, length, text, attrs);
    }

    @Override public void remove(FilterBypass fb, int offset, int length) throws BadLocationException { super.remove(fb, offset, length); }

    private boolean isAcceptable(String candidate) {
        if (candidate.isEmpty()) return true;
        if (!candidate.matches("[0-9.]*")) return false;
        if (candidate.chars().filter(ch -> ch == '.').count() > 1) return false;
        return true;
    }
}

/* InputVerifier that ensures value is a float between min and max (inclusive) */
class NumericRangeVerifier extends InputVerifier {
    private final float min, max;
    public NumericRangeVerifier(float min, float max) { this.min = min; this.max = max; }
    @Override
    public boolean verify(JComponent input) {
        if (input instanceof JFormattedTextField) {
            JFormattedTextField tf = (JFormattedTextField) input;
            String txt = tf.getText().trim();
            if (txt.isEmpty()) return false;
            try {
                float v = Float.parseFloat(txt);
                return v >= min && v <= max;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }
    @Override
    public boolean shouldYieldFocus(JComponent input) {
        boolean ok = verify(input);
        if (!ok) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(input),
                    "Please enter a valid CGPA between " + String.format("%.2f", min) + " and " + String.format("%.2f", max) + ".",
                    "Invalid CGPA", JOptionPane.WARNING_MESSAGE);
            input.requestFocusInWindow();
        }
        return ok;
    }
}

/* Generic Pattern-based verifier for ID/Name */
class PatternVerifier extends InputVerifier {
    private final Pattern pattern;
    private final String errorMessage;
    public PatternVerifier(String regex, String errorMessage) { this.pattern = Pattern.compile(regex); this.errorMessage = errorMessage; }
    @Override
    public boolean verify(JComponent input) {
        if (input instanceof JTextField) {
            String txt = ((JTextField) input).getText().trim();
            // IMPORTANT: allow empty here so that the verifier does not pop up an error immediately when the dialog opens
            // Empty checks are performed explicitly in dialog handlers (add/edit) so the user sees a friendly message there.
            if (txt.isEmpty()) return true;
            return pattern.matcher(txt).matches();
        }
        return true;
    }
    @Override
    public boolean shouldYieldFocus(JComponent input) {
        boolean ok = verify(input);
        if (!ok) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(input), errorMessage, "Invalid input", JOptionPane.WARNING_MESSAGE);
            input.requestFocusInWindow();
        }
        return ok;
    }
}

/* Manager with persistence */
class PlacementManager {
    private Map<String, Student> students = new HashMap<>();
    private Map<String, Company> companies = new HashMap<>();
    private List<PlacementRecord> records = new ArrayList<>();
    private final String STUD_FILE = "students.dat";
    private final String COMP_FILE = "companies.dat";
    private final String REC_FILE = "records.dat";

    @SuppressWarnings("unchecked")
    public void loadAll() {
        students = (Map<String, Student>) load(STUD_FILE, new HashMap<String, Student>());
        companies = (Map<String, Company>) load(COMP_FILE, new HashMap<String, Company>());
        records = (List<PlacementRecord>) load(REC_FILE, new ArrayList<PlacementRecord>());
    }

    public void saveAll() {
        save(STUD_FILE, students);
        save(COMP_FILE, companies);
        save(REC_FILE, records);
    }

    private Object load(String f, Object def) {
        try (ObjectInputStream o = new ObjectInputStream(new FileInputStream(f))) { return o.readObject(); }
        catch (Exception e) { return def; }
    }
    private void save(String f, Object o) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) { out.writeObject(o); }
        catch (Exception e) { System.err.println("Save error: " + e.getMessage()); }
    }

    public void addStudent(Student s) throws InvalidDataException {
        if (students.containsKey(s.getId())) throw new InvalidDataException("Student ID exists!");
        students.put(s.getId(), s);
    }
    public void updateStudent(String id, Student s) throws RecordNotFoundException {
        if (!students.containsKey(id)) throw new RecordNotFoundException("Student not found!");
        students.put(id, s);
    }
    public void addCompany(Company c) throws InvalidDataException {
        if (companies.containsKey(c.getId())) throw new InvalidDataException("Company ID exists!");
        companies.put(c.getId(), c);
    }
    public void updateCompany(String id, Company c) throws RecordNotFoundException {
        if (!companies.containsKey(id)) throw new RecordNotFoundException("Company not found!");
        companies.put(id, c);
    }
    public Student getStudent(String id) throws RecordNotFoundException {
        Student s = students.get(id); if (s == null) throw new RecordNotFoundException("Student not found!"); return s;
    }
    public Company getCompany(String id) throws RecordNotFoundException {
        Company c = companies.get(id); if (c == null) throw new RecordNotFoundException("Company not found!"); return c;
    }
    public void removeStudent(String id) { students.remove(id); }
    public void removeCompany(String id) { companies.remove(id); }
    public void addPlacementRecord(PlacementRecord pr) { records.add(pr); saveAll(); }
    public List<Student> getAllStudents() { return new ArrayList<>(students.values()); }
    public List<Company> getAllCompanies() { return new ArrayList<>(companies.values()); }
    public List<PlacementRecord> getAllRecords() { return records; }

    private boolean hasRequiredSkills(Student s, Company c) {
        for (Skill req : c.getRequiredSkills()) {
            boolean found = false;
            for (Skill own : s.getSkills()) if (own.getName().equalsIgnoreCase(req.getName())) { found = true; break; }
            if (!found) return false;
        }
        return true;
    }

    public List<Student> getEligibleStudents(String companyId) throws RecordNotFoundException {
        Company c = getCompany(companyId);
        List<Student> eligible = new ArrayList<>();
        for (Student s : students.values())
            if (s.getCgpa() >= c.getMinCgpa() && hasRequiredSkills(s, c))
                eligible.add(s);
        return eligible;
    }

    public List<Student> getSortedByCGPA() {
        List<Student> list = getAllStudents();
        list.sort(Comparator.comparingDouble(Student::getCgpa).reversed());
        return list;
    }
}

/* Placement drive thread */
class PlacementDrive implements Runnable {
    private Student s;
    private Company c;
    private PlacementManager m;
    public PlacementDrive(Student s, Company c, PlacementManager m) { this.s = s; this.c = c; this.m = m; }
    public void run() {
        try {
            Thread.sleep(500 + (int) (Math.random() * 800));
            String result = Math.random() > 0.5 ? "Selected" : "Rejected";
            m.addPlacementRecord(new PlacementRecord(s.getId(), c.getId(), result));
            System.out.println("Interview: " + s.getName() + " -> " + result);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

/* GUI */
class PlacementGUI extends JFrame {
    private PlacementManager m;

    // Branch options
    private static final String[] BRANCHES = { "Computer Science and Engineering", "Computer Science (AI&ML)",
            "Information Technology", "Electronic Communication Engineering", "Mechanical Engineering",
            "Civil Engineering" };

    // Reusable verifiers
    private final PatternVerifier idVerifier = new PatternVerifier("^[0-9-]+$",
            "Invalid ID. Only digits (0-9) and '-' are allowed.");
    private final PatternVerifier nameVerifier = new PatternVerifier("^[A-Za-z ]+$",
            "Invalid Name. Only alphabetic letters and spaces are allowed.");
    private final NumericRangeVerifier cgpaVerifier = new NumericRangeVerifier(0.0f, 10.0f);

    public PlacementGUI(PlacementManager m) {
        this.m = m;
        setTitle("Placement Management System");
        setSize(1050, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(8, 8, 0, 8));
        JLabel title = new JLabel("Placement Management System", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Top area: 3 columns of controls
        JPanel columnsPanel = new JPanel(new GridLayout(1, 3, 12, 12));
        columnsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Column 1: Student actions (top) + Company actions (below) stacked vertically
        JPanel col1 = new JPanel();
        col1.setLayout(new BoxLayout(col1, BoxLayout.Y_AXIS));
        JPanel studentGroup = new JPanel(new GridLayout(0, 1, 8, 8));
        studentGroup.setBorder(new TitledBorder("Student Actions"));
        JPanel companyGroup = new JPanel(new GridLayout(0, 1, 8, 8));
        companyGroup.setBorder(new TitledBorder("Company Actions"));
        col1.add(studentGroup);
        col1.add(Box.createRigidArea(new Dimension(0, 12)));
        col1.add(companyGroup);

        // Column 2: Skills
        JPanel col2 = new JPanel(new BorderLayout());
        JPanel skillGroup = new JPanel(new GridLayout(0, 1, 8, 8));
        skillGroup.setBorder(new TitledBorder("Skills"));
        col2.add(skillGroup, BorderLayout.NORTH);

        // Column 3: Drive & Reports
        JPanel driveGroup = new JPanel(new GridLayout(0, 1, 8, 8));
        driveGroup.setBorder(new TitledBorder("Drive & Reports"));

        columnsPanel.add(col1);
        columnsPanel.add(col2);
        columnsPanel.add(driveGroup);

        add(columnsPanel, BorderLayout.CENTER);

        // Build buttons and wire actions
        addButtons(studentGroup, companyGroup, skillGroup, driveGroup);

        setVisible(true);
    }

    // helper: show a popup dialog with provided text (scrollable)
    private void showPopup(String title, String content) {
        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(700, 400));
        JDialog dlg = new JDialog(this, title, true);
        dlg.add(sp);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // list -> convert to text and show popup
    private void showListPopup(String title, List<?> list) {
        StringBuilder sb = new StringBuilder();
        if (list.isEmpty()) sb.append("No records found.\n");
        else for (Object o : list) sb.append(o.toString()).append("\n");
        showPopup(title, sb.toString());
    }

    private void addButtons(JPanel studentGroup, JPanel companyGroup, JPanel skillGroup, JPanel driveGroup) {
        Font btnFont = new Font("SansSerif", Font.PLAIN, 14);
        Dimension btnDim = new Dimension(300, 38);

        JButton addStudent = new JButton("Add Student");
        JButton editStudent = new JButton("Edit Student");
        JButton removeStudent = new JButton("Remove Student");

        for (JButton b : new JButton[] { addStudent, editStudent, removeStudent }) {
            b.setFont(btnFont);
            b.setPreferredSize(btnDim);
            studentGroup.add(b);
        }

        JButton addCompany = new JButton("Add Company");
        JButton editCompany = new JButton("Edit Company");
        JButton removeCompany = new JButton("Remove Company");
        for (JButton b : new JButton[] { addCompany, editCompany, removeCompany }) {
            b.setFont(btnFont);
            b.setPreferredSize(btnDim);
            companyGroup.add(b);
        }

        JButton addSkill = new JButton("Add Skill to Student");
        JButton addReqSkill = new JButton("Add Required Skill to Company");
        for (JButton b : new JButton[] { addSkill, addReqSkill }) {
            b.setFont(btnFont);
            b.setPreferredSize(btnDim);
            skillGroup.add(b);
        }

        JButton showStudents = new JButton("Show Students");
        JButton showCompanies = new JButton("Show Companies");
        JButton eligible = new JButton("Show Eligible Students");
        JButton drive = new JButton("Start Placement Drive");
        JButton records = new JButton("Show Placement Records");
        JButton sorted = new JButton("Show Students Sorted by CGPA");

        for (JButton b : new JButton[] { showStudents, showCompanies, eligible, drive, records, sorted }) {
            b.setFont(btnFont);
            b.setPreferredSize(btnDim);
            driveGroup.add(b);
        }

        // Tooltips
        addStudent.setToolTipText("Add a new student record (ID, name, branch, CGPA).");
        editStudent.setToolTipText("Edit existing student details by ID.");
        removeStudent.setToolTipText("Remove a student by ID.");

        addCompany.setToolTipText("Add a new company.");
        editCompany.setToolTipText("Edit company details by ID.");
        removeCompany.setToolTipText("Remove company by ID.");

        addSkill.setToolTipText("Add a skill to a student.");
        addReqSkill.setToolTipText("Add a required skill to a company.");

        showStudents.setToolTipText("List all students in a popup window.");
        showCompanies.setToolTipText("List all companies in a popup window.");
        eligible.setToolTipText("Show students eligible for a given company in popup.");
        drive.setToolTipText("Run placement interviews for eligible students and show summary.");
        records.setToolTipText("Show all placement records in a popup.");
        sorted.setToolTipText("Show students sorted by CGPA (descending) in popup.");

        // Action listeners: reuse your existing dialog methods
        addStudent.addActionListener(e -> addStudentDialog());
        editStudent.addActionListener(e -> editStudentDialog());
        removeStudent.addActionListener(e -> removeStudentDialog());
        addCompany.addActionListener(e -> addCompanyDialog());
        editCompany.addActionListener(e -> editCompanyDialog());
        removeCompany.addActionListener(e -> removeCompanyDialog());
        addSkill.addActionListener(e -> addSkillDialog());
        addReqSkill.addActionListener(e -> addRequiredSkillDialog());
        showStudents.addActionListener(e -> showListPopup("All Students", m.getAllStudents()));
        showCompanies.addActionListener(e -> showListPopup("All Companies", m.getAllCompanies()));
        eligible.addActionListener(e -> showEligibleStudents());
        drive.addActionListener(e -> startDriveAndShowSummary());
        records.addActionListener(e -> showListPopup("Placement Records", m.getAllRecords()));
        sorted.addActionListener(e -> showListPopup("Students by CGPA", m.getSortedByCGPA()));
    }

    private JSpinner createCgpaSpinner(double initial, boolean unusedRtl) {
        SpinnerNumberModel model = new SpinnerNumberModel(initial, 0.0, 10.0, 0.01);
        JSpinner spinner = new JSpinner(model);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.00");
        spinner.setEditor(editor);
        JFormattedTextField tf = editor.getTextField();
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        tf.setInputVerifier(cgpaVerifier);

        // Always left aligned so the number and caret are at left (easier to edit and consistent UI)
        tf.setHorizontalAlignment(SwingConstants.LEFT);
        tf.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        return spinner;
    }

    private int showDialogWithFocus(Object[] components, String title, final JComponent focusComponent,
            final boolean focusCaretAtEnd) {
        JOptionPane pane = new JOptionPane(components, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(this, title);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                try {
                    if (focusComponent instanceof JSpinner) {
                        JSpinner sp = (JSpinner) focusComponent;
                        if (sp.getEditor() instanceof JSpinner.NumberEditor) {
                            JFormattedTextField tf = ((JSpinner.NumberEditor) sp.getEditor()).getTextField();
                            tf.requestFocusInWindow();
                            if (focusCaretAtEnd) {
                                tf.setCaretPosition(tf.getText().length());
                                tf.select(tf.getText().length(), tf.getText().length());
                            } else {
                                tf.setCaretPosition(0);
                                tf.select(0, 0);
                            }
                        } else {
                            focusComponent.requestFocusInWindow();
                        }
                    } else {
                        focusComponent.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    focusComponent.requestFocusInWindow();
                }
            }
        });
        dialog.setVisible(true);
        Object selectedValue = pane.getValue();
        if (selectedValue == null) return JOptionPane.CLOSED_OPTION;
        if (selectedValue instanceof Integer) return ((Integer) selectedValue).intValue();
        return JOptionPane.CLOSED_OPTION;
    }

    private void addStudentDialog() {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        id.setInputVerifier(idVerifier);
        name.setInputVerifier(nameVerifier);
        JComboBox<String> branchCombo = new JComboBox<>(BRANCHES);
        branchCombo.setSelectedIndex(0);

        // CGPA spinner (left aligned)
        JSpinner cgpaSpinner = createCgpaSpinner(8.00, false);

        Object[] f = { "ID:", id, "Name:", name, "Branch:", branchCombo, "CGPA:", cgpaSpinner };
        int res = showDialogWithFocus(f, "Add Student", cgpaSpinner, false);

        if (res == JOptionPane.OK_OPTION) {
            try {
                String sid = id.getText() == null ? "" : id.getText().trim();
                String sname = name.getText() == null ? "" : name.getText().trim();
                if (sid.isEmpty() || sname.isEmpty()) {
                    showPopup("Input required", "Student ID and Name are required. Please provide both.");
                    return;
                }

                if (!idVerifier.verify(id)) { idVerifier.shouldYieldFocus(id); return; }
                if (!nameVerifier.verify(name)) { nameVerifier.shouldYieldFocus(name); return; }

                JFormattedTextField tf = ((JSpinner.NumberEditor) cgpaSpinner.getEditor()).getTextField();
                InputVerifier v = tf.getInputVerifier();
                if (v != null && !v.verify(tf)) { v.shouldYieldFocus(tf); return; }

                String sbranch = (String) branchCombo.getSelectedItem();
                float val = ((Number) cgpaSpinner.getValue()).floatValue();

                Student s = new Student(sid, sname, sbranch, val);
                m.addStudent(s);
                m.saveAll();
                showPopup("Student Added", "Student added:\n" + s.toString());

            } catch (InvalidDataException ide) {
                showPopup("Error", ide.getMessage());
            } catch (Exception e) {
                showPopup("Error", "Error adding student: " + e.getMessage());
            }
        }
    }

    private void editStudentDialog() {
        String idVal = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (idVal == null || idVal.trim().isEmpty()) return;
        try {
            Student sOld = m.getStudent(idVal.trim());
            JLabel idLabel = new JLabel(idVal.trim());
            JTextField nameField = new JTextField(sOld.getName());
            nameField.setInputVerifier(nameVerifier);
            JComboBox<String> branchCombo = new JComboBox<>(BRANCHES);
            branchCombo.setSelectedItem(sOld.getBranch());
            JSpinner cgpaSpinner = createCgpaSpinner(sOld.getCgpa(), false);

            Object[] f = { "ID:", idLabel, "Name:", nameField, "Branch:", branchCombo, "CGPA:", cgpaSpinner };
            int res = showDialogWithFocus(f, "Edit Student - " + idVal.trim(), cgpaSpinner, false);
            if (res != JOptionPane.OK_OPTION) { showPopup("Info", "Operation cancelled."); return; }

            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                showPopup("Input required", "Name is required.");
                return;
            }
            if (!nameVerifier.verify(nameField)) { nameVerifier.shouldYieldFocus(nameField); return; }

            JFormattedTextField tf = ((JSpinner.NumberEditor) cgpaSpinner.getEditor()).getTextField();
            InputVerifier v = tf.getInputVerifier();
            if (v != null && !v.verify(tf)) { v.shouldYieldFocus(tf); return; }

            String newName = nameField.getText().trim();
            String newBranch = (String) branchCombo.getSelectedItem();
            float val = ((Number) cgpaSpinner.getValue()).floatValue();

            Student sNew = new Student(idVal.trim(), newName, newBranch, val);
            for (Skill sk : sOld.getSkills()) sNew.addSkill(sk);

            m.updateStudent(idVal.trim(), sNew);
            m.saveAll();
            showPopup("Student Updated", "Updated:\n" + sNew.toString());
        } catch (RecordNotFoundException rnfe) {
            showPopup("Error", rnfe.getMessage());
        } catch (Exception e) {
            showPopup("Error", "Error updating student: " + e.getMessage());
        }
    }

    private void removeStudentDialog() {
        String id = JOptionPane.showInputDialog(this, "Enter Student ID to remove:");
        if (id == null || id.trim().isEmpty()) return;
        m.removeStudent(id.trim());
        m.saveAll();
        showPopup("Student Removed", "Student removed (if existed): " + id.trim());
    }

    private void addCompanyDialog() {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JTextField role = new JTextField();
        id.setInputVerifier(idVerifier);
        name.setInputVerifier(nameVerifier);
        JSpinner cgpaSpinner = createCgpaSpinner(7.00, false);

        Object[] f = { "ID:", id, "Name:", name, "Role:", role, "Min CGPA:", cgpaSpinner };
        int res = showDialogWithFocus(f, "Add Company", cgpaSpinner, false);
        if (res == JOptionPane.OK_OPTION) {
            try {
                // explicit empty checks
                String cid = id.getText() == null ? "" : id.getText().trim();
                String cname = name.getText() == null ? "" : name.getText().trim();
                if (cid.isEmpty() || cname.isEmpty()) {
                    showPopup("Input required", "Company ID and Name are required. Please provide both.");
                    return;
                }

                if (!idVerifier.verify(id)) { idVerifier.shouldYieldFocus(id); return; }
                if (!nameVerifier.verify(name)) { nameVerifier.shouldYieldFocus(name); return; }

                JFormattedTextField tf = ((JSpinner.NumberEditor) cgpaSpinner.getEditor()).getTextField();
                InputVerifier v = tf.getInputVerifier();
                if (v != null && !v.verify(tf)) { v.shouldYieldFocus(tf); return; }

                String crole = role.getText().trim();
                float val = ((Number) cgpaSpinner.getValue()).floatValue();

                Company c = new Company(cid, cname, crole, val);
                m.addCompany(c);
                m.saveAll();
                showPopup("Company Added", "Company added:\n" + c.toString());
            } catch (InvalidDataException ide) {
                showPopup("Error", ide.getMessage());
            } catch (Exception e) {
                showPopup("Error", "Error adding company: " + e.getMessage());
            }
        }
    }

    private void editCompanyDialog() {
        String idVal = JOptionPane.showInputDialog(this, "Enter Company ID:");
        if (idVal == null || idVal.trim().isEmpty()) return;
        try {
            Company old = m.getCompany(idVal.trim());
            JLabel idLabel = new JLabel(idVal.trim());
            JTextField nameField = new JTextField(old.getName());
            nameField.setInputVerifier(nameVerifier);
            JTextField roleField = new JTextField(old.getRole());
            JSpinner cgpaSpinner = createCgpaSpinner(old.getMinCgpa(), false);

            Object[] f = { "ID:", idLabel, "Name:", nameField, "Role:", roleField, "Min CGPA:", cgpaSpinner };
            int res = showDialogWithFocus(f, "Edit Company - " + idVal.trim(), cgpaSpinner, false);
            if (res != JOptionPane.OK_OPTION) { showPopup("Info", "Operation cancelled."); return; }

            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                showPopup("Input required", "Name is required.");
                return;
            }
            if (!nameVerifier.verify(nameField)) { nameVerifier.shouldYieldFocus(nameField); return; }

            JFormattedTextField tf = ((JSpinner.NumberEditor) cgpaSpinner.getEditor()).getTextField();
            InputVerifier v = tf.getInputVerifier();
            if (v != null && !v.verify(tf)) { v.shouldYieldFocus(tf); return; }

            String newName = nameField.getText().trim();
            String newRole = roleField.getText().trim();
            float val = ((Number) cgpaSpinner.getValue()).floatValue();

            Company updated = new Company(idVal.trim(), newName, newRole, val);
            for (Skill sk : old.getRequiredSkills()) updated.addRequiredSkill(sk);

            m.updateCompany(idVal.trim(), updated);
            m.saveAll();
            showPopup("Company Updated", "Updated:\n" + updated.toString());
        } catch (RecordNotFoundException rnfe) {
            showPopup("Error", rnfe.getMessage());
        } catch (Exception e) {
            showPopup("Error", "Error updating company: " + e.getMessage());
        }
    }

    private void removeCompanyDialog() {
        String id = JOptionPane.showInputDialog(this, "Enter Company ID to remove:");
        if (id == null || id.trim().isEmpty()) return;
        m.removeCompany(id.trim());
        m.saveAll();
        showPopup("Company Removed", "Company removed (if existed): " + id.trim());
    }

    private void addSkillDialog() {
        String sid = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (sid == null || sid.trim().isEmpty()) return;
        try {
            Student s = m.getStudent(sid.trim());
            String skill = JOptionPane.showInputDialog(this, "Skill name:");
            if (skill == null || skill.trim().isEmpty()) { showPopup("Info", "No skill entered. Operation cancelled."); return; }
            String[] options = { "Low", "High" };
            int sel = JOptionPane.showOptionDialog(this, "Select skill level for \"" + skill.trim() + "\":",
                    "Skill Level", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (sel < 0) { showPopup("Info", "Operation cancelled."); return; }
            String level = options[sel];
            s.addSkill(new Skill(skill.trim(), level));
            m.saveAll();
            showPopup("Skill Added", "Skill added to " + s.getName() + " (" + level + ")");
        } catch (RecordNotFoundException rnfe) {
            showPopup("Error", rnfe.getMessage());
        } catch (Exception e) {
            showPopup("Error", "Error adding skill: " + e.getMessage());
        }
    }

    private void addRequiredSkillDialog() {
        String cid = JOptionPane.showInputDialog(this, "Enter Company ID:");
        if (cid == null || cid.trim().isEmpty()) return;
        try {
            Company c = m.getCompany(cid.trim());
            String skill = JOptionPane.showInputDialog(this, "Required skill:");
            if (skill == null || skill.trim().isEmpty()) { showPopup("Info", "No skill entered. Operation cancelled."); return; }
            String[] options = { "Low", "High" };
            int sel = JOptionPane.showOptionDialog(this, "Select preferred level for \"" + skill.trim() + "\":",
                    "Preferred Skill Level", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (sel < 0) { showPopup("Info", "Operation cancelled."); return; }
            String level = options[sel];
            c.addRequiredSkill(new Skill(skill.trim(), level));
            m.saveAll();
            showPopup("Skill Added", "Skill added to " + c.getName() + " (" + level + ")");
        } catch (RecordNotFoundException rnfe) {
            showPopup("Error", rnfe.getMessage());
        } catch (Exception e) {
            showPopup("Error", "Error adding required skill: " + e.getMessage());
        }
    }

    private void showEligibleStudents() {
        String cid = JOptionPane.showInputDialog(this, "Enter Company ID:");
        if (cid == null || cid.trim().isEmpty()) return;
        try {
            List<Student> eligible = m.getEligibleStudents(cid.trim());
            showListPopup("Eligible Students for " + cid.trim(), eligible);
        } catch (Exception e) {
            showPopup("Error", "Error: " + e.getMessage());
        }
    }

    private void startDriveAndShowSummary() {
        String cid = JOptionPane.showInputDialog(this, "Enter Company ID for Drive:");
        if (cid == null || cid.trim().isEmpty()) return;
        try {
            Company c = m.getCompany(cid.trim());
            List<Student> eligible = m.getEligibleStudents(cid.trim());
            if (eligible.isEmpty()) { showPopup("Drive Summary", "No eligible students!"); return; }
            List<String> summary = new ArrayList<>();
            for (Student s : eligible) {
                String result = Math.random() > 0.5 ? "Selected" : "Rejected";
                m.addPlacementRecord(new PlacementRecord(s.getId(), c.getId(), result));
                summary.add(s.getName() + " (" + s.getId() + ") -> " + result);
            }
            m.saveAll();
            StringBuilder sb = new StringBuilder();
            sb.append("Placement Drive Results for company: ").append(c.getName()).append(" (" + c.getId() + ")\n\n");
            for (String line : summary) sb.append(line).append("\n");
            showPopup("Drive Summary", sb.toString());
        } catch (Exception e) {
            showPopup("Error", "Error: " + e.getMessage());
        }
    }
}

/* Main */
public class PlacementManagementSystem {
    public static void main(String[] args) {
        PlacementManager m = new PlacementManager();
        m.loadAll();
        SwingUtilities.invokeLater(() -> new PlacementGUI(m));
    }
}