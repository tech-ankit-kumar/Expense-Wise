import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.*;

public class ExpenseWise {
    enum Theme { LIGHT, DARK, GRADIENT }
    static Theme theme = Theme.LIGHT;
    static Color BG, CARD, TEXT, MUTED, BORDER, ACCENT, GREEN, RED, SIDEBAR, SIDEBAR_TEXT;
    static Color INPUT_BG, SECOND;
    static void setPalette(){
        if(theme==Theme.DARK){
            BG=new Color(7,14,31); CARD=new Color(15,30,55); TEXT=new Color(244,247,255); MUTED=new Color(154,170,198);
            BORDER=new Color(35,57,91); ACCENT=new Color(80,100,255); GREEN=new Color(38,214,145); RED=new Color(255,74,116);
            SIDEBAR=new Color(5,13,32); SIDEBAR_TEXT=new Color(235,241,255);
            INPUT_BG=new Color(11,23,46); SECOND=new Color(56,199,187);
        } else if(theme==Theme.GRADIENT){
            BG=new Color(239,243,255); CARD=Color.WHITE; TEXT=new Color(24,34,61); MUTED=new Color(92,107,138);
            BORDER=new Color(218,226,247); ACCENT=new Color(83,70,229); GREEN=new Color(16,185,129); RED=new Color(244,63,94);
            SIDEBAR=new Color(22,25,69); SIDEBAR_TEXT=new Color(244,246,255);
            INPUT_BG=new Color(245,244,255); SECOND=new Color(217,70,239);
        } else {
            BG=new Color(245,248,253); CARD=Color.WHITE; TEXT=new Color(23,31,52); MUTED=new Color(102,116,145);
            BORDER=new Color(222,229,241); ACCENT=new Color(67,84,224); GREEN=new Color(14,180,126); RED=new Color(235,65,103);
            SIDEBAR=new Color(11,20,43); SIDEBAR_TEXT=new Color(238,243,255);
            INPUT_BG=new Color(247,248,255); SECOND=new Color(13,148,136);
        }
    }
    static final String[] CATS={"Food","Travel","Shopping","Bills","Entertainment","Health","Education","Others"};
    static final Color[] CAT_COLORS={new Color(65,105,255),new Color(22,201,141),new Color(255,178,26),new Color(123,92,255),new Color(255,79,145),new Color(14,165,233),new Color(168,85,247),new Color(147,164,199)};
    static Color catColor(String cat){ for(int i=0;i<CATS.length;i++) if(CATS[i].equals(cat)) return CAT_COLORS[i]; return MUTED; }
    static Color catColorByLabel(String label){ for(int i=0;i<CATS.length;i++) if(catLabel(CATS[i]).equals(label)) return CAT_COLORS[i]; return MUTED; }
    static final DateTimeFormatter DF=DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter DISPLAY=DateTimeFormatter.ofPattern("dd MMM yyyy");
    static final Path DIR=Paths.get(System.getProperty("user.home"),".expensewise");
    static final Path DATA=DIR.resolve("data.tsv"), USERS=DIR.resolve("users.txt"), PREF=DIR.resolve("prefs.properties"), BUDGET=DIR.resolve("budgets.properties"), USERDETAILS=DIR.resolve("userdetails.tsv"), PHOTOS_DIR=DIR.resolve("photos");
    static final long MAX_PHOTO_BYTES=100*1024;
    static final List<Tx> txs=new ArrayList<Tx>(); static final List<String> users=new ArrayList<String>(); static String user="Default", lang="English";
    static String currency="₹";
    static final String[] CURRENCIES={"₹","$","€","£","¥"};
    static final Map<String,String> CURRENCY_NAMES=new LinkedHashMap<String,String>();
    static { CURRENCY_NAMES.put("₹","INR — Indian Rupee"); CURRENCY_NAMES.put("$","USD — US Dollar"); CURRENCY_NAMES.put("€","EUR — Euro"); CURRENCY_NAMES.put("£","GBP — British Pound"); CURRENCY_NAMES.put("¥","JPY — Japanese Yen"); }
    static final Map<String,String> userEmail=new HashMap<String,String>(); static final Map<String,String> userPhone=new HashMap<String,String>(); static final Map<String,String> userPassword=new HashMap<String,String>(); static final Map<String,String> userPhoto=new HashMap<String,String>();
    static final Map<String,BufferedImage> photoCache=new HashMap<String,BufferedImage>();
    static final Map<String,BufferedImage> circleCache=new HashMap<String,BufferedImage>();
    static JFrame frame; static JPanel content; static JLabel pageTitle, pageSubtitle; static String activeNav="Dashboard"; static final List<JButton> NAV_BUTTONS=new ArrayList<JButton>(); static JTable table; static DefaultTableModel model;
    static JPanel rootPanel, sidebarPanel; static JButton hamburgerBtn; static boolean narrowMode=false, sidebarOpen=true;
    static final int NARROW_BREAKPOINT=980;
    static JTextField searchField,fromField,toField; static JComboBox<String> catFilter,monthFilter,typeFilter; static JLabel status;
    static Tx lastDeletedTx=null; static JButton undoDeleteBtn=null;
    static List<Tx> currentDisplayTxs=new ArrayList<Tx>();

    static final String[] LANGS={"English","Hindi","Spanish","French","German","Japanese"};
    static final Map<String, Map<String,String>> LANG_MAP=new HashMap<String, Map<String,String>>();
    static {
        Map<String,String> es=new HashMap<String,String>();
        es.put("Dashboard","Panel"); es.put("Add Transaction","Añadir movimiento"); es.put("Transactions","Movimientos");
        es.put("Recurring","Recurrente"); es.put("Reports & Export","Informes"); es.put("Settings","Ajustes");
        es.put("Logout","Cerrar sesión"); es.put("Exit","Salir"); es.put("Language","Idioma");
        es.put("Welcome back — here is your financial overview.","Bienvenido — aquí está tu resumen financiero.");
        es.put("Total balance","Saldo total"); es.put("Total income","Ingresos"); es.put("Total expenses","Gastos");
        es.put("This month","Este mes"); es.put("Net position","Posición neta");
        es.put("All recorded income","Ingresos registrados"); es.put("All recorded expenses","Gastos registrados");
        es.put("Current month spend","Gasto del mes");
        es.put("Monthly financial health","Salud financiera mensual"); es.put("available balance","saldo disponible");
        es.put("Keep an eye on recurring costs and monthly limits.","Vigila gastos recurrentes y límites mensuales.");
        es.put("Add expense","Añadir gasto"); es.put("Add income","Añadir ingreso");
        es.put("Cash flow","Flujo de caja"); es.put("Last 6 months","Últimos 6 meses");
        es.put("Spending by Category","Gasto por categoría");
        es.put("Recent transactions","Movimientos recientes"); es.put("Monthly budget","Presupuesto mensual");
        es.put("No budget set","Sin presupuesto"); es.put("Set a budget in Settings.","Configura un presupuesto.");
        es.put("used this month","usado este mes"); es.put("Set budget","Configurar presupuesto");
        es.put("Quick actions","Acciones rápidas"); es.put("Export to PDF","Exportar a PDF");
        es.put("Search by description...","Buscar por descripción...");
        es.put("From (yyyy-mm-dd)","Desde (aaaa-mm-dd)"); es.put("To (yyyy-mm-dd)","Hasta (aaaa-mm-dd)");
        es.put("All categories","Todas las categorías"); es.put("All months","Todos los meses"); es.put("All types","Todos los tipos");
        es.put("Clear","Limpiar"); es.put("Edit","Editar"); es.put("Delete","Eliminar");
        es.put("Serial No.","N.º"); es.put("Date","Fecha"); es.put("Description","Descripción");
        es.put("Category","Categoría"); es.put("Type","Tipo"); es.put("Amount","Importe"); es.put("Recurring","Recurrente");
        es.put("Yes","Sí"); es.put("No","No"); es.put("Income","Ingreso"); es.put("Expense","Gasto");
        es.put("Food & Dining","Comida"); es.put("Travel","Viaje"); es.put("Shopping","Compras");
        es.put("Bills & Utilities","Facturas"); es.put("Entertainment","Entretenimiento");
        es.put("Health","Salud"); es.put("Education","Educación"); es.put("Others","Otros");
        es.put("Appearance","Apariencia"); es.put("Choose the theme you like. It is saved automatically.","Elige el tema. Se guarda automáticamente.");
        es.put("Light","Claro"); es.put("Dark","Oscuro"); es.put("Gradient","Degradado");
        es.put("Set a spending limit for the current user.","Establece un límite de gasto.");
        es.put("Add Budget","Añadir presupuesto"); es.put("Reset data","Restablecer datos");
        es.put("Reset Budget","Restablecer presupuesto"); es.put("Clear Income","Borrar ingresos"); es.put("Clear Expense","Borrar gastos");
        es.put("User profiles","Perfil"); es.put("Edit my profile","Editar mi perfil");
        LANG_MAP.put("Spanish", es);
        Map<String,String> fr=new HashMap<String,String>();
        fr.put("Dashboard","Tableau de bord"); fr.put("Add Transaction","Ajouter"); fr.put("Transactions","Transactions");
        fr.put("Recurring","Récurrent"); fr.put("Reports & Export","Rapports"); fr.put("Settings","Paramètres");
        fr.put("Logout","Déconnexion"); fr.put("Exit","Quitter"); fr.put("Language","Langue");
        fr.put("Welcome back — here is your financial overview.","Bon retour — voici votre aperçu financier.");
        fr.put("Total balance","Solde total"); fr.put("Total income","Revenus totaux"); fr.put("Total expenses","Dépenses totales");
        fr.put("This month","Ce mois"); fr.put("Net position","Position nette");
        fr.put("All recorded income","Tous les revenus"); fr.put("All recorded expenses","Toutes les dépenses");
        fr.put("Current month spend","Dépenses du mois");
        fr.put("Monthly financial health","Santé financière mensuelle"); fr.put("available balance","solde disponible");
        fr.put("Add expense","Ajouter dépense"); fr.put("Add income","Ajouter revenu");
        fr.put("Cash flow","Trésorerie"); fr.put("Last 6 months","6 derniers mois");
        fr.put("Spending by Category","Dépenses par catégorie");
        fr.put("Recent transactions","Transactions récentes"); fr.put("Monthly budget","Budget mensuel");
        fr.put("No budget set","Aucun budget"); fr.put("Set budget","Définir le budget");
        fr.put("Quick actions","Actions rapides"); fr.put("Export to PDF","Exporter en PDF");
        fr.put("Search by description...","Rechercher...");
        fr.put("All categories","Toutes catégories"); fr.put("All months","Tous les mois"); fr.put("All types","Tous types");
        fr.put("Clear","Effacer"); fr.put("Edit","Modifier"); fr.put("Delete","Supprimer");
        fr.put("Serial No.","N°"); fr.put("Date","Date"); fr.put("Description","Description");
        fr.put("Category","Catégorie"); fr.put("Type","Type"); fr.put("Amount","Montant"); fr.put("Recurring","Récurrent");
        fr.put("Yes","Oui"); fr.put("No","Non"); fr.put("Income","Revenu"); fr.put("Expense","Dépense");
        fr.put("Food & Dining","Alimentation"); fr.put("Travel","Voyage"); fr.put("Shopping","Achats");
        fr.put("Bills & Utilities","Factures"); fr.put("Entertainment","Loisirs");
        fr.put("Health","Santé"); fr.put("Education","Éducation"); fr.put("Others","Autres");
        fr.put("Appearance","Apparence"); fr.put("Light","Clair"); fr.put("Dark","Sombre"); fr.put("Gradient","Dégradé");
        fr.put("User profiles","Profil"); fr.put("Edit my profile","Modifier mon profil");
        LANG_MAP.put("French", fr);
        Map<String,String> de=new HashMap<String,String>();
        de.put("Dashboard","Übersicht"); de.put("Add Transaction","Hinzufügen"); de.put("Transactions","Transaktionen");
        de.put("Recurring","Wiederkehrend"); de.put("Reports & Export","Berichte"); de.put("Settings","Einstellungen");
        de.put("Logout","Abmelden"); de.put("Exit","Beenden"); de.put("Language","Sprache");
        de.put("Welcome back — here is your financial overview.","Willkommen zurück — hier ist Ihre Finanzübersicht.");
        de.put("Total balance","Gesamtsaldo"); de.put("Total income","Gesamteinnahmen"); de.put("Total expenses","Gesamtausgaben");
        de.put("This month","Dieser Monat"); de.put("Net position","Nettoposition");
        de.put("All recorded income","Alle Einnahmen"); de.put("All recorded expenses","Alle Ausgaben");
        de.put("Current month spend","Ausgaben dieses Monats");
        de.put("Monthly financial health","Monatliche Finanzlage"); de.put("available balance","verfügbares Guthaben");
        de.put("Add expense","Ausgabe hinzufügen"); de.put("Add income","Einnahme hinzufügen");
        de.put("Cash flow","Cashflow"); de.put("Last 6 months","Letzte 6 Monate");
        de.put("Spending by Category","Ausgaben nach Kategorie");
        de.put("Recent transactions","Letzte Transaktionen"); de.put("Monthly budget","Monatsbudget");
        de.put("No budget set","Kein Budget"); de.put("Set budget","Budget festlegen");
        de.put("Quick actions","Schnellaktionen"); de.put("Export to PDF","Als PDF exportieren");
        de.put("Search by description...","Nach Beschreibung suchen...");
        de.put("All categories","Alle Kategorien"); de.put("All months","Alle Monate"); de.put("All types","Alle Typen");
        de.put("Clear","Löschen"); de.put("Edit","Bearbeiten"); de.put("Delete","Löschen");
        de.put("Serial No.","Nr."); de.put("Date","Datum"); de.put("Description","Beschreibung");
        de.put("Category","Kategorie"); de.put("Type","Typ"); de.put("Amount","Betrag"); de.put("Recurring","Wiederkehrend");
        de.put("Yes","Ja"); de.put("No","Nein"); de.put("Income","Einnahme"); de.put("Expense","Ausgabe");
        de.put("Food & Dining","Essen"); de.put("Travel","Reise"); de.put("Shopping","Einkaufen");
        de.put("Bills & Utilities","Rechnungen"); de.put("Entertainment","Unterhaltung");
        de.put("Health","Gesundheit"); de.put("Education","Bildung"); de.put("Others","Sonstiges");
        de.put("Appearance","Erscheinungsbild"); de.put("Light","Hell"); de.put("Dark","Dunkel"); de.put("Gradient","Verlauf");
        de.put("User profiles","Profil"); de.put("Edit my profile","Profil bearbeiten");
        LANG_MAP.put("German", de);
        Map<String,String> ja=new HashMap<String,String>();
        ja.put("Dashboard","ダッシュボード"); ja.put("Add Transaction","追加"); ja.put("Transactions","取引");
        ja.put("Recurring","定期"); ja.put("Reports & Export","レポート"); ja.put("Settings","設定");
        ja.put("Logout","ログアウト"); ja.put("Exit","終了"); ja.put("Language","言語");
        ja.put("Welcome back — here is your financial overview.","おかえりなさい — 財務の概要です。");
        ja.put("Total balance","総残高"); ja.put("Total income","総収入"); ja.put("Total expenses","総支出");
        ja.put("This month","今月"); ja.put("Net position","純資産");
        ja.put("Add expense","支出を追加"); ja.put("Add income","収入を追加");
        ja.put("Cash flow","キャッシュフロー"); ja.put("Last 6 months","過去6ヶ月");
        ja.put("Spending by Category","カテゴリ別支出");
        ja.put("Recent transactions","最近の取引"); ja.put("Monthly budget","月次予算");
        ja.put("Quick actions","クイック操作"); ja.put("Export to PDF","PDFエクスポート");
        ja.put("Search by description...","説明で検索...");
        ja.put("All categories","すべてのカテゴリ"); ja.put("All months","すべての月"); ja.put("All types","すべての種類");
        ja.put("Clear","クリア"); ja.put("Edit","編集"); ja.put("Delete","削除");
        ja.put("Serial No.","番号"); ja.put("Date","日付"); ja.put("Description","説明");
        ja.put("Category","カテゴリ"); ja.put("Type","種類"); ja.put("Amount","金額"); ja.put("Recurring","定期");
        ja.put("Yes","はい"); ja.put("No","いいえ"); ja.put("Income","収入"); ja.put("Expense","支出");
        ja.put("Food & Dining","食事"); ja.put("Travel","旅行"); ja.put("Shopping","買い物");
        ja.put("Bills & Utilities","請求"); ja.put("Entertainment","娯楽");
        ja.put("Health","健康"); ja.put("Education","教育"); ja.put("Others","その他");
        ja.put("Appearance","外観"); ja.put("Light","ライト"); ja.put("Dark","ダーク"); ja.put("Gradient","グラデーション");
        ja.put("User profiles","プロフィール"); ja.put("Edit my profile","プロフィール編集");
        LANG_MAP.put("Japanese", ja);
    }

    static class Tx { long id; String user,type,date,category,description; double amount; boolean recurring;
        Tx(long i,String u,String ty,String d,String c,String de,double a,boolean r){id=i;user=u;type=ty;date=d;category=c;description=de;amount=a;recurring=r;}
    }
    public static void main(String[] args){ SwingUtilities.invokeLater(new Runnable(){public void run(){start();}}); }
    static void start(){
        load(); materializeRecurring(); save(); setPalette(); applyLook();
        frame=new JFrame("ExpenseWise — Personal Finance");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(380,520));
        GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle screen=ge.getMaximumWindowBounds();
        frame.setSize(Math.min(1320,screen.width),Math.min(820,screen.height));
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        showLockScreen();
        frame.addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){updateResponsiveSidebar();}});
        javax.swing.Timer narrowTimer=new javax.swing.Timer(250,new ActionListener(){public void actionPerformed(ActionEvent e){updateResponsiveSidebar();}});
        narrowTimer.start();
        frame.setVisible(true);
        updateResponsiveSidebar();
    }
    static void applyLook(){
        try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ignored){}
        applyUiManagerColors();
    }
    static void applyUiManagerColors(){
        UIManager.put("Table.showGrid",false); UIManager.put("Button.focus",new Color(0,0,0,0));
        UIManager.put("ComboBox.background",CARD); UIManager.put("ComboBox.foreground",TEXT);
        UIManager.put("ComboBox.selectionBackground",ACCENT); UIManager.put("ComboBox.selectionForeground",Color.WHITE);
        UIManager.put("ComboBox.buttonBackground",CARD); UIManager.put("ComboBox.buttonShadow",BORDER); UIManager.put("ComboBox.buttonDarkShadow",BORDER); UIManager.put("ComboBox.buttonHighlight",CARD);
        UIManager.put("TextField.background",CARD); UIManager.put("TextField.foreground",TEXT); UIManager.put("TextField.caretForeground",TEXT); UIManager.put("TextField.inactiveForeground",MUTED); UIManager.put("TextField.inactiveBackground",CARD);
        UIManager.put("TextField.selectionColor",ACCENT); UIManager.put("TextField.selectedTextColor",Color.WHITE);
        UIManager.put("FormattedTextField.background",CARD); UIManager.put("FormattedTextField.foreground",TEXT);
        UIManager.put("PasswordField.background",CARD); UIManager.put("PasswordField.foreground",TEXT);
        UIManager.put("TextArea.background",CARD); UIManager.put("TextArea.foreground",TEXT);
        UIManager.put("List.background",CARD); UIManager.put("List.foreground",TEXT); UIManager.put("List.selectionBackground",ACCENT); UIManager.put("List.selectionForeground",Color.WHITE);
        UIManager.put("Table.background",CARD); UIManager.put("Table.foreground",TEXT); UIManager.put("Table.selectionBackground",ACCENT); UIManager.put("Table.selectionForeground",Color.WHITE); UIManager.put("Table.gridColor",BORDER);
        UIManager.put("TableHeader.background",CARD); UIManager.put("TableHeader.foreground",TEXT);
        UIManager.put("ScrollPane.background",BG); UIManager.put("Viewport.background",BG);
        UIManager.put("PopupMenu.background",CARD); UIManager.put("MenuItem.background",CARD); UIManager.put("MenuItem.foreground",TEXT);
        UIManager.put("OptionPane.background",CARD); UIManager.put("OptionPane.messageForeground",TEXT); UIManager.put("Panel.background",BG);
    }
    static void build(){
        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(BG);
        rootPanel=root;
        frame.setContentPane(root);
        if(narrowMode && sidebarOpen){
            sidebarPanel=null;
            JPanel drawer=new JPanel(new BorderLayout());
            drawer.setBackground(SIDEBAR);
            drawer.setPreferredSize(new Dimension(220,0));
            JPanel closeBar=new JPanel(new BorderLayout());
            closeBar.setBackground(SIDEBAR);
            closeBar.setBorder(new EmptyBorder(16,20,4,20));
            JLabel drawerTitle=text(t("Navigation","नेविगेशन"),13,Font.BOLD,SIDEBAR_TEXT);
            closeBar.add(drawerTitle,BorderLayout.WEST);
            JButton close=new FlatButton("✕",10);
            close.setFont(font(Font.BOLD,16));
            close.setForeground(SIDEBAR_TEXT);
            close.setBackground(new Color(255,255,255,25));
            close.setBorder(new EmptyBorder(6,14,6,14));
            close.setPreferredSize(new Dimension(44,36));
            close.setToolTipText(t("Close menu","मेन्यू बंद करें"));
            close.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){ sidebarOpen=false; rebuild(); }});
            closeBar.add(close,BorderLayout.EAST);
            drawer.add(closeBar,BorderLayout.NORTH);
            JPanel sb=sidebar();
            sb.setPreferredSize(null);
            drawer.add(sb,BorderLayout.CENTER);
            root.add(drawer,BorderLayout.WEST);
            root.add(mainArea(),BorderLayout.CENTER);
            showActive();
        } else {
            sidebarPanel=sidebar();
            if(!narrowMode){
                sidebarPanel.setVisible(true);
                root.add(sidebarPanel,BorderLayout.WEST);
            } else {
                sidebarPanel=null;
            }
            root.add(mainArea(),BorderLayout.CENTER);
            showActive();
        }
    }
    static void updateResponsiveSidebar(){
        if(frame==null)return;
        int w=frame.getContentPane()!=null?frame.getContentPane().getWidth():frame.getWidth();
        boolean nowNarrow=w<NARROW_BREAKPOINT;
        if(nowNarrow!=narrowMode){
            narrowMode=nowNarrow;
            sidebarOpen=!narrowMode;
            rebuild();
        }
    }
    static void applySidebarVisibility(){
        if(narrowMode){ rebuild(); return; }
        if(sidebarPanel==null)return;
        sidebarPanel.setVisible(sidebarOpen);
        if(rootPanel!=null){rootPanel.revalidate();rootPanel.repaint();}
    }
    static void showActive(){
        if("Add".equals(activeNav))showAdd();
        else if("Transactions".equals(activeNav))showTransactions();
        else if("Recurring".equals(activeNav))showRecurring();
        else if("Reports".equals(activeNav))showReports();
        else if("Settings".equals(activeNav))showSettings();
        else showDashboard();
    }
    static Font font(int style,int size){return new Font("SansSerif",style,size);}
    static JLabel text(String s,int size,int style,Color c){ JLabel l=new JLabel(s); l.setFont(font(style,size)); l.setForeground(c); return l; }
    static JPanel rounded(final int radius,final Color bg){ return new JPanel(){ { setOpaque(false); } protected void paintComponent(Graphics g){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bg); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius); g2.dispose(); super.paintComponent(g); }}; }
    static JPanel card(){
        final int shadow=6, radius=18;
        JPanel p=new JPanel(){
            { setOpaque(false); }
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(), h=getHeight();
                for(int i=shadow;i>=1;i--){
                    g2.setColor(new Color(15,23,42, 3+i*2));
                    g2.fillRoundRect(i, i+2, Math.max(1,w-shadow-i), Math.max(1,h-shadow-i), radius, radius);
                }
                g2.setColor(CARD);
                g2.fillRoundRect(0,0,w-shadow,h-shadow,radius,radius);
                g2.setColor(BORDER);
                g2.drawRoundRect(0,0,w-shadow-1,h-shadow-1,radius,radius);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setBorder(new EmptyBorder(18,20,18,20));
        return p;
    }
    static class FlatButton extends JButton{
        int radius;
        FlatButton(String s,int radius){ super(s); this.radius=radius; setContentAreaFilled(false); setOpaque(false); setBorderPainted(false); setFocusPainted(false); }
        protected void paintComponent(Graphics g){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(getBackground()); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius); g2.dispose(); super.paintComponent(g); }
    }
    static class PlaceholderField extends JTextField{
        String placeholder;
        PlaceholderField(String ph){ super(); placeholder=ph; }
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(getText().length()==0 && placeholder!=null){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MUTED);
                Insets ins=getInsets(); int avail=getWidth()-ins.left-ins.right-4;
                Font pf=getFont(); FontMetrics fm=g2.getFontMetrics(pf); String show=placeholder;
                while(fm.stringWidth(show)>avail && pf.getSize()>9){ pf=pf.deriveFont((float)(pf.getSize()-1)); fm=g2.getFontMetrics(pf); }
                if(fm.stringWidth(show)>avail){ while(show.length()>1 && fm.stringWidth(show+"\u2026")>avail) show=show.substring(0,show.length()-1); show=show+"\u2026"; }
                g2.setFont(pf);
                int y=(getHeight()-fm.getHeight())/2+fm.getAscent();
                int x=ins.left+2;
                if(getHorizontalAlignment()==JTextField.CENTER){ int textW=fm.stringWidth(show); x=Math.max(ins.left+2,(getWidth()-textW)/2); }
                g2.drawString(show,x,y); g2.dispose();
            }
        }
    }
    static class FocusBorder extends AbstractBorder {
        final int radius;
        FocusBorder(int radius){ this.radius=radius; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h){
            boolean focused=c.hasFocus();
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(focused?ACCENT:BORDER);
            g2.setStroke(new BasicStroke(focused?2f:1f));
            g2.drawRoundRect(x+1,y+1,w-3,h-3,radius,radius);
            g2.dispose();
        }
        public Insets getBorderInsets(Component c){ return new Insets(9,11,9,11); }
        public Insets getBorderInsets(Component c,Insets ins){ ins.set(9,11,9,11); return ins; }
        public boolean isBorderOpaque(){ return false; }
    }
    static JButton button(String s,final Color bg,Color fg){ final JButton b=new FlatButton(s,10); b.setFont(font(Font.BOLD,12)); b.setForeground(fg); b.setBackground(bg); b.setBorder(new EmptyBorder(10,16,10,16));
        final Color hover=new Color(Math.min(255,bg.getRed()+16),Math.min(255,bg.getGreen()+16),Math.min(255,bg.getBlue()+16),bg.getAlpha());
        b.addMouseListener(new MouseAdapter(){ public void mouseEntered(MouseEvent e){ b.setBackground(hover); } public void mouseExited(MouseEvent e){ b.setBackground(bg); } });
        return b; }
    static JLabel pill(String s,Color c){ JLabel l=text(s,11,Font.BOLD,c); l.setOpaque(true); l.setBorder(new CompoundBorder(new LineBorder(new Color(c.getRed(),c.getGreen(),c.getBlue(),45),1,true),new EmptyBorder(5,9,5,9))); l.setBackground(new Color(c.getRed(),c.getGreen(),c.getBlue(),20)); return l; }
    static String money(double v){return currency+String.format(Locale.US,"%,.2f",v);}
    static String t(String en,String hi){
        if("Hindi".equals(lang)) return hi;
        Map<String,String> m=LANG_MAP.get(lang);
        if(m!=null){ String v=m.get(en); if(v!=null) return v; }
        return en;
    }
    static String catLabel(String c){
        if("Food".equals(c))return t("Food & Dining","भोजन और खानपान");
        if("Travel".equals(c))return t("Travel","यात्रा");
        if("Shopping".equals(c))return t("Shopping","शॉपिंग");
        if("Bills".equals(c))return t("Bills & Utilities","बिल और उपयोगिताएं");
        if("Entertainment".equals(c))return t("Entertainment","मनोरंजन");
        if("Health".equals(c))return t("Health","स्वास्थ्य");
        if("Education".equals(c))return t("Education","शिक्षा");
        if("Others".equals(c))return t("Others","अन्य");
        if("Income".equals(c))return t("Income","आय");
        return c;
    }
    static String typeLabel(String ty){
        if("Income".equals(ty))return t("Income","आय");
        if("Expense".equals(ty))return t("Expense","खर्च");
        return ty;
    }
    static void localizeCategoryCombo(JComboBox<String> box){ box.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean isSelected,boolean cellHasFocus){ Component c=super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus); if(value!=null)setText(catLabel(String.valueOf(value))); return c; }}); }
    static void localizeTypeCombo(JComboBox<String> box){ box.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean isSelected,boolean cellHasFocus){ Component c=super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus); if(value!=null)setText(typeLabel(String.valueOf(value))); return c; }}); }

    static JPanel sidebar(){
        JPanel p=new JPanel(new BorderLayout()); p.setPreferredSize(new Dimension(286,0)); p.setBackground(SIDEBAR); p.setBorder(new EmptyBorder(26,18,18,18));
        JPanel top=new JPanel(); top.setOpaque(false); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        JPanel brand=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); brand.setOpaque(false);
        JLabel logo=text("◆",26,Font.BOLD,new Color(101,90,255)); logo.setPreferredSize(new Dimension(38,42)); logo.setHorizontalAlignment(SwingConstants.CENTER);
        brand.add(logo); brand.add(Box.createHorizontalStrut(10));
        JPanel bt=new JPanel(); bt.setOpaque(false); bt.setLayout(new BoxLayout(bt,BoxLayout.Y_AXIS));
        bt.add(text("ExpenseWise",22,Font.BOLD,Color.WHITE)); bt.add(Box.createVerticalStrut(2)); bt.add(text(t("Smart Personal Finance","स्मार्ट पर्सनल फाइनेंस"),11,Font.PLAIN,new Color(158,172,202)));
        brand.add(bt); top.add(brand); top.add(Box.createVerticalStrut(30));
        NAV_BUTTONS.clear();
        JPanel navWrap=new JPanel(new BorderLayout()); navWrap.setOpaque(false);
        JPanel navList=new JPanel(new GridLayout(7,1,0,8)); navList.setOpaque(false);
        navList.add(navButton("⌂","Dashboard",t("Dashboard","डैशबोर्ड"),new Runnable(){public void run(){showDashboard();}}));
        navList.add(navButton("＋","Add",t("Add Transaction","लेन-देन जोड़ें"),new Runnable(){public void run(){showAdd();}}));
        navList.add(navButton("▤","Transactions",t("Transactions","लेन-देन"),new Runnable(){public void run(){showTransactions();}}));
        navList.add(navButton("◷","Recurring",t("Recurring","आवर्ती"),new Runnable(){public void run(){showRecurring();}}));
        navList.add(navButton("▥","Reports",t("Reports & Export","रिपोर्ट व एक्सपोर्ट"),new Runnable(){public void run(){showReports();}}));
        navList.add(navButton("⚙","Settings",t("Settings","सेटिंग्स"),new Runnable(){public void run(){showSettings();}}));
        navList.add(navButton("⎋","Logout",t("Logout","लॉगआउट"),new Runnable(){public void run(){ logout(); }}));
        navWrap.add(navList,BorderLayout.CENTER); top.add(navWrap);
        top.add(Box.createVerticalGlue());
        JPanel quick=quickActionsPanel(); top.add(Box.createVerticalStrut(20)); top.add(quick);
        JButton exitBtn=navButton("⏻","Exit",t("Exit","बाहर निकलें"),new Runnable(){public void run(){ exitApp(); }});
        exitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,NAV_BUTTON_H));
        JPanel exitWrap=new JPanel(new BorderLayout()); exitWrap.setOpaque(false);
        exitWrap.setBorder(new EmptyBorder(10,0,0,0));
        exitWrap.add(exitBtn,BorderLayout.CENTER);
        top.add(exitWrap);
        p.add(top,BorderLayout.CENTER);
        return p;
    }
    static JLabel clip(JLabel l,int maxWidth){ l.setMaximumSize(new Dimension(maxWidth,l.getPreferredSize().height)); l.setPreferredSize(new Dimension(Math.min(maxWidth,l.getPreferredSize().width),l.getPreferredSize().height)); return l; }
    static JPanel quickActionsPanel(){
        Color panelBg = theme==Theme.DARK ? new Color(15,30,55) : (theme==Theme.GRADIENT ? new Color(43,40,105) : new Color(225,232,247));
        Color labelColor = theme!=Theme.LIGHT ? SIDEBAR_TEXT : TEXT;
        JPanel wrap=rounded(14,panelBg); wrap.setBorder(new EmptyBorder(12,12,12,12)); wrap.setLayout(new BorderLayout(0,10));
        wrap.add(text(t("Quick actions","त्वरित कार्य"),11,Font.BOLD,labelColor),BorderLayout.NORTH);
        JPanel colWrap=new JPanel(new BorderLayout());colWrap.setOpaque(false);
        JPanel col=new JPanel(new GridLayout(3,1,0,8)); col.setOpaque(false);
        col.add(quickActionButton("＋",t("Add expense","खर्च जोड़ें"),RED,new Runnable(){public void run(){showAdd("Expense");}}));
        col.add(quickActionButton("＋",t("Add income","आय जोड़ें"),GREEN,new Runnable(){public void run(){showAdd("Income");}}));
        col.add(quickActionButton("⭳",t("Export to PDF","PDF में एक्सपोर्ट करें"),ACCENT,new Runnable(){public void run(){exportPdf(filtered());}}));
        colWrap.add(col,BorderLayout.CENTER); wrap.add(colWrap,BorderLayout.CENTER); return wrap;
    }
    static JButton quickActionButton(String icon,String label,final Color accent,final Runnable r){
        JButton b=new FlatButton(icon+"   "+label,9);
        b.setFont(font(Font.BOLD,13)); b.setForeground(accent); b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setPreferredSize(new Dimension(10,NAV_BUTTON_H));
        b.setBackground(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),28));
        b.setBorder(new CompoundBorder(new LineBorder(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),80),1,true),new EmptyBorder(8,12,8,12)));
        b.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){r.run();}}); return b;
    }

    static BufferedImage makeCircleAvatar(BufferedImage src, int size){
        if(src==null||size<=0) return null;
        int iw=src.getWidth(), ih=src.getHeight();
        int side=Math.min(iw,ih);
        int sx=(iw-side)/2, sy=(ih-side)/2;
        BufferedImage square;
        try{ square=(sx==0&&sy==0&&side==iw&&side==ih)?src:src.getSubimage(sx,sy,side,side); }
        catch(Exception e){ square=src; }
        BufferedImage cur=square;
        int cw=cur.getWidth();
        while(cw>size*2){
            int nw=Math.max(size,cw/2);
            BufferedImage tmp=new BufferedImage(nw,nw,BufferedImage.TYPE_INT_ARGB);
            Graphics2D gt=tmp.createGraphics();
            gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gt.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            gt.drawImage(cur,0,0,nw,nw,null);
            gt.dispose();
            cur=tmp; cw=nw;
        }
        BufferedImage out=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2=out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        g2.setClip(new Ellipse2D.Float(0,0,size,size));
        g2.drawImage(cur,0,0,size,size,null);
        g2.dispose();
        return out;
    }
    static JComponent personAvatar(final int size,final Color bg,final String username){
        JPanel p=new JPanel(){ 
            protected void paintComponent(Graphics g){ 
                super.paintComponent(g); 
                Graphics2D g2=(Graphics2D)g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
                int w=getWidth(),h=getHeight();
                BufferedImage photo=getUserPhoto(username);
                if(photo!=null){
                    String key=username+"@"+w+"x"+h;
                    BufferedImage circ=circleCache.get(key);
                    if(circ==null){ circ=makeCircleAvatar(photo,Math.min(w,h)); circleCache.put(key,circ); }
                    g2.drawImage(circ,0,0,null);
                    g2.dispose();
                    return;
                }
                g2.setColor(bg); g2.fillOval(0,0,w-1,h-1);
                g2.setColor(Color.WHITE); 
                int headD=(int)Math.round(w*0.34); int headX=(w-headD)/2, headY=(int)Math.round(h*0.18); 
                g2.fillOval(headX,headY,headD,headD);
                int bodyW=(int)Math.round(w*0.62), bodyH=(int)Math.round(h*0.5); 
                int bodyX=(w-bodyW)/2, bodyY=(int)Math.round(h*0.52);
                Shape old=g2.getClip(); 
                g2.clip(new Rectangle(0,0,w,h)); 
                g2.fillOval(bodyX,bodyY,bodyW,bodyH); 
                g2.setClip(old); 
                g2.dispose(); 
            } 
        };
        p.setOpaque(false); p.setPreferredSize(new Dimension(size,size)); return p;
    }
    static BufferedImage getUserPhoto(String username){ if(username==null) return null; if(photoCache.containsKey(username)) return photoCache.get(username); BufferedImage img=null; String path=userPhoto.get(username); if(path!=null){ try{ img=ImageIO.read(new File(path)); }catch(Exception ignored){} } photoCache.put(username,img); return img; }
    static void choosePhoto(final String username,final Runnable onChange){
        JFileChooser fc=new JFileChooser();
        fc.setDialogTitle(t("Choose profile picture","प्रोफ़ाइल फोटो चुनें"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(t("Images (max 100 KB)","इमेज (अधिकतम 100 KB)"),"png","jpg","jpeg","gif","bmp"));
        if(fc.showOpenDialog(frame)!=JFileChooser.APPROVE_OPTION) return;
        File f=fc.getSelectedFile();
        try{
            long sz=Files.size(f.toPath());
            if(sz>MAX_PHOTO_BYTES){ JOptionPane.showMessageDialog(frame,t("Please choose an image smaller than 100 KB.","कृपया 100 KB से छोटी फोटो चुनें।"),"ExpenseWise",JOptionPane.ERROR_MESSAGE); return; }
            BufferedImage test=ImageIO.read(f);
            if(test==null){ JOptionPane.showMessageDialog(frame,t("That file doesn't look like a valid image.","यह फ़ाइल एक मान्य इमेज नहीं लगती।"),"ExpenseWise",JOptionPane.ERROR_MESSAGE); return; }
            Files.createDirectories(PHOTOS_DIR);
            String name=f.getName(); int dot=name.lastIndexOf('.'); String ext=dot>=0?name.substring(dot):"";
            Path dest=PHOTOS_DIR.resolve(UUID.randomUUID().toString()+ext);
            Files.copy(f.toPath(),dest,StandardCopyOption.REPLACE_EXISTING);
            String old=userPhoto.get(username); userPhoto.put(username,dest.toString()); photoCache.remove(username);
            Iterator<String> it=circleCache.keySet().iterator();
            while(it.hasNext()){ if(it.next().startsWith(username+"@")) it.remove(); }
            if(old!=null){ try{Files.deleteIfExists(Paths.get(old));}catch(Exception ignored){} }
            save(); if(onChange!=null) onChange.run();
        }catch(IOException ex){ JOptionPane.showMessageDialog(frame,t("Could not read that file: ","फ़ाइल नहीं पढ़ी जा सकी: ")+ex.getMessage(),"ExpenseWise",JOptionPane.ERROR_MESSAGE); }
    }
    static void removePhoto(String username){
        String old=userPhoto.remove(username); photoCache.remove(username);
        Iterator<String> it=circleCache.keySet().iterator();
        while(it.hasNext()){ if(it.next().startsWith(username+"@")) it.remove(); }
        if(old!=null){ try{Files.deleteIfExists(Paths.get(old));}catch(Exception ignored){} } save();
    }
    static void deleteAccount(String target){
        List<Tx> rm=new ArrayList<Tx>();
        for(Tx x:txs) if(x.user.equals(target)) rm.add(x);
        txs.removeAll(rm);
        users.remove(target);
        userEmail.remove(target); userPhone.remove(target); userPassword.remove(target);
        removePhoto(target);
        try{
            Properties bp=new Properties();
            if(Files.exists(BUDGET)){InputStream i=Files.newInputStream(BUDGET);bp.load(i);i.close();}
            bp.remove(target);
            OutputStream o=Files.newOutputStream(BUDGET);bp.store(o,"ExpenseWise budgets");o.close();
        }catch(IOException ignored){}
        if(target.equals(loadRememberedUser())) saveRememberedUser("");
        if(users.isEmpty()) users.add("Default");
        if(user.equals(target)) user=users.get(0);
        save();
        showLockScreen();
    }
    static void logout(){
    int c=JOptionPane.showConfirmDialog(frame,t("Log out of ExpenseWise?","ExpenseWise से लॉगआउट करें?"),t("Logout","लॉगआउट"),JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
    if(c!=JOptionPane.YES_OPTION)return;
    save();showLockScreen();
}
    static final int NAV_BUTTON_H=46;
    static JButton navButton(final String icon,final String key,final String label,final Runnable r){
        final JButton b=new FlatButton(icon+"    "+label,10); b.putClientProperty("navKey",key); NAV_BUTTONS.add(b); b.setPreferredSize(new Dimension(10,NAV_BUTTON_H)); b.setHorizontalAlignment(SwingConstants.LEFT); b.setFont(font(Font.BOLD,13)); b.setForeground(SIDEBAR_TEXT); b.setBackground(SIDEBAR); b.setBorder(new EmptyBorder(10,14,10,14));
        b.addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){if(!key.equals(activeNav))b.setBackground(new Color(255,255,255,18));} public void mouseExited(MouseEvent e){if(!key.equals(activeNav))b.setBackground(SIDEBAR);}});
        b.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            activeNav=key;updateNavButtons();r.run();
            if(narrowMode){ sidebarOpen=false; rebuild(); }
        }}); updateNavButtons(); return b;
    }
    static void updateNavButtons(){for(JButton b:NAV_BUTTONS){boolean active=activeNav.equals(b.getClientProperty("navKey"));b.setBackground(active?new Color(58,75,185):SIDEBAR);b.setForeground(active?Color.WHITE:SIDEBAR_TEXT);b.setBorder(active?new CompoundBorder(new LineBorder(new Color(93,107,255),1,true),new EmptyBorder(9,13,9,13)):new EmptyBorder(10,14,10,14));}}
    static class HamburgerButton extends FlatButton{
        HamburgerButton(){super("",10);}
        protected void paintComponent(Graphics g){ super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(getForeground()); g2.setStroke(new BasicStroke(2.2f)); int w=getWidth(),h=getHeight(); int lw=Math.min(18,w-16); int x=(w-lw)/2,cy=h/2; g2.drawLine(x,cy-6,x+lw,cy-6); g2.drawLine(x,cy,x+lw,cy); g2.drawLine(x,cy+6,x+lw,cy+6); g2.dispose(); }
    }
    static JPanel mainArea(){ JPanel p=new JPanel(new BorderLayout()); p.setBackground(BG); p.add(topbar(),BorderLayout.NORTH); content=new JPanel(new BorderLayout()); content.setBackground(BG); p.add(content,BorderLayout.CENTER); return p; }

    static JPanel topbar(){
        JPanel p=new JPanel(new BorderLayout(0,10)); p.setBackground(CARD); p.setBorder(new CompoundBorder(new MatteBorder(0,0,2,0,BORDER),new EmptyBorder(20,30,20,30)));
        JPanel leftWrap=new JPanel(new BorderLayout(14,0)); leftWrap.setOpaque(false);
        hamburgerBtn=new HamburgerButton(); hamburgerBtn.setForeground(TEXT); hamburgerBtn.setBackground(BG); hamburgerBtn.setBorder(new EmptyBorder(8,12,8,12)); hamburgerBtn.setPreferredSize(new Dimension(40,40)); hamburgerBtn.setVisible(narrowMode);
        hamburgerBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            sidebarOpen=!sidebarOpen;
            if(narrowMode) rebuild();
            else applySidebarVisibility();
        }});
        leftWrap.add(hamburgerBtn,BorderLayout.WEST);
        JPanel left=new JPanel(); left.setOpaque(false); left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        pageTitle=text(t("Dashboard","डैशबोर्ड"),24,Font.BOLD,TEXT);
        pageSubtitle=text(t("Welcome back — here is your financial overview.","वापसी पर स्वागत है — आपके वित्त का सारांश।"),12,Font.PLAIN,MUTED);
        left.add(pageTitle); left.add(Box.createVerticalStrut(4)); left.add(pageSubtitle); leftWrap.add(left,BorderLayout.CENTER);

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,6)); right.setOpaque(false);
        JPanel chip=new JPanel(new BorderLayout(10,0));
        chip.setOpaque(true);
        chip.setBackground(INPUT_BG);
        chip.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true), new EmptyBorder(5,8,5,14)));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.add(personAvatar(34,new Color(118,102,255),user),BorderLayout.WEST);
        JPanel pd=new JPanel(); pd.setOpaque(false); pd.setLayout(new BoxLayout(pd,BoxLayout.Y_AXIS));
        JLabel nameLbl=text(user,12,Font.BOLD,TEXT);
        pd.add(nameLbl);
        String pdEmail=userEmail.containsKey(user)?userEmail.get(user):"";
        String pdPhone=userPhone.containsKey(user)?userPhone.get(user):"";
        String sub=pdEmail.length()>0?pdEmail:(pdPhone.length()>0?pdPhone:t("Personal","व्यक्तिगत"));
        pd.add(text(sub,10,Font.PLAIN,MUTED));
        chip.add(pd,BorderLayout.CENTER);
        chip.setToolTipText(t("Edit my profile","मेरी प्रोफ़ाइल संपादित करें"));
        chip.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ editUserDialog(user); } });
        right.add(chip);

        if(narrowMode){
            JPanel stack=new JPanel(); stack.setOpaque(false); stack.setLayout(new BoxLayout(stack,BoxLayout.Y_AXIS));
            leftWrap.setAlignmentX(Component.LEFT_ALIGNMENT); right.setAlignmentX(Component.LEFT_ALIGNMENT);
            stack.add(leftWrap); stack.add(right); p.add(stack,BorderLayout.CENTER);
        } else {
            p.add(leftWrap,BorderLayout.WEST); p.add(right,BorderLayout.EAST);
        }
        return p;
    }
    static void styleCombo(final JComboBox<?> c){
        c.setFont(font(Font.PLAIN,12)); c.setBackground(CARD); c.setForeground(TEXT);
        c.setBorder(new LineBorder(BORDER,1,true)); c.setOpaque(true);
        c.setRenderer(new DefaultListCellRenderer(){ public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focused){ JLabel l=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused); l.setFont(font(Font.PLAIN,12)); l.setOpaque(true); if(selected){l.setBackground(ACCENT);l.setForeground(Color.WHITE);}else{l.setBackground(CARD);l.setForeground(TEXT);} l.setBorder(new EmptyBorder(5,8,5,8)); return l; } });
    }
    static JPanel sectionHeader(String s){
        JPanel h=new JPanel(new BorderLayout(10,0)); h.setOpaque(false);
        JPanel bar=new JPanel(){ protected void paintComponent(Graphics g){ Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(ACCENT); g2.fillRoundRect(0,0,getWidth(),getHeight(),4,4); g2.dispose(); } };
        bar.setOpaque(false); bar.setPreferredSize(new Dimension(5,20));
        JPanel barWrap=new JPanel(new GridBagLayout()); barWrap.setOpaque(false); barWrap.add(bar);
        h.add(barWrap,BorderLayout.WEST); h.add(text(s,15,Font.BOLD,TEXT),BorderLayout.CENTER);
        return h;
    }
    static JPanel pageShell(String title,String subtitle){ JPanel p=new JPanel(new BorderLayout(0,18)); p.setBackground(BG); p.setBorder(new EmptyBorder(22,26,22,26)); pageTitle.setText(title); pageSubtitle.setText(subtitle); return p; }
    static void setContent(JPanel p){
        if(frame!=null && frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(null);
        content.removeAll(); content.add(p,BorderLayout.CENTER); content.revalidate(); content.repaint();
    }
    static class WidthTrackingPanel extends JPanel implements Scrollable{
        WidthTrackingPanel(LayoutManager lm){super(lm);}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 16;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return 100;}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    static JScrollPane vScroll(JComponent innerContent){ WidthTrackingPanel wrap=new WidthTrackingPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(innerContent,BorderLayout.CENTER); JScrollPane sp=new JScrollPane(wrap); sp.setBorder(null); sp.setOpaque(false); sp.getViewport().setOpaque(false); sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); sp.getVerticalScrollBar().setUnitIncrement(16); return sp; }

    static void showDashboard(){
        JPanel p=pageShell(t("Dashboard","डैशबोर्ड"),t("Welcome back — here is your financial overview.","वापसी पर स्वागत है — आपके वित्त का सारांश।"));
        JPanel scrollBody=new JPanel(); scrollBody.setOpaque(false); scrollBody.setLayout(new BoxLayout(scrollBody,BoxLayout.Y_AXIS));
        double income=sum("Income"), expense=sum("Expense"), balance=income-expense;
        double thisMonthIncome=monthIncome(), thisMonthExpense=monthExpense();
        JPanel stats=new JPanel(narrowMode?new GridLayout(4,1,0,10):new GridLayout(1,4,14,0)); stats.setOpaque(false);
        stats.add(statCard(t("Total balance","कुल बैलेंस"),t("Net position","शुद्ध स्थिति"),money(balance),ACCENT,"↗"));
        stats.add(statCard(t("Total income","कुल आय"),t("All recorded income","सभी दर्ज आय"),money(income),GREEN,"＋"));
        stats.add(statCard(t("Total expenses","कुल खर्च"),t("All recorded expenses","सभी दर्ज खर्च"),money(expense),RED,"−"));
        stats.add(monthCard(thisMonthIncome, thisMonthExpense));
        scrollBody.add(stats); scrollBody.add(Box.createVerticalStrut(16));
        JPanel hero=new GradientPanel(); hero.setBorder(new EmptyBorder(20,24,20,24)); hero.setLayout(new BorderLayout(20,14)); JPanel hv=new JPanel(); hv.setOpaque(false); hv.setLayout(new BoxLayout(hv,BoxLayout.Y_AXIS)); hv.add(text(t("Financial health","वित्तीय स्थिति"),13,Font.BOLD,Color.WHITE)); hv.add(Box.createVerticalStrut(7)); hv.add(text(money(income-expense)+"  "+t("available balance","उपलब्ध बैलेंस"),28,Font.BOLD,Color.WHITE)); hv.add(Box.createVerticalStrut(6)); hv.add(text(t("Keep an eye on recurring costs and monthly limits.","आवर्ती खर्च और मासिक बजट पर नज़र रखें।"),12,Font.PLAIN,new Color(255,255,255,190))); hero.add(hv,narrowMode?BorderLayout.NORTH:BorderLayout.WEST); JPanel actions=new JPanel(new FlowLayout(narrowMode?FlowLayout.LEFT:FlowLayout.RIGHT,8,0)); actions.setOpaque(false); JButton ae=button("＋ "+t("Add expense","खर्च जोड़ें"),Color.WHITE,new Color(190,24,93)); ae.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){showAdd("Expense");}}); JButton ai=button("＋ "+t("Add income","आय जोड़ें"),new Color(255,255,255,55),Color.WHITE); ai.setBorder(new CompoundBorder(new LineBorder(new Color(255,255,255,100),1,true),new EmptyBorder(9,15,9,15))); ai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){showAdd("Income");}}); actions.add(ae); actions.add(ai); hero.add(actions,narrowMode?BorderLayout.SOUTH:BorderLayout.EAST); scrollBody.add(hero); scrollBody.add(Box.createVerticalStrut(16));
        JPanel charts=new JPanel(narrowMode?new GridLayout(2,1,0,14):new GridLayout(1,2,14,0)); charts.setOpaque(false); charts.add(monthlyChartCard()); charts.add(categoryChartCard()); scrollBody.add(charts); scrollBody.add(Box.createVerticalStrut(16));
        JPanel bottom=new JPanel(narrowMode?new GridLayout(2,1,0,14):new GridLayout(1,2,14,0)); bottom.setOpaque(false); bottom.add(recentCard()); bottom.add(budgetCard()); scrollBody.add(bottom);
        JScrollPane sp=vScroll(scrollBody); p.add(sp,BorderLayout.CENTER); setContent(p);
    }
    static JPanel statCard(String title,String sub,String value,Color c,String icon){
        JPanel p=card(); p.setLayout(new BorderLayout(14,0)); p.setBorder(new EmptyBorder(16,18,16,18));
        JPanel iconBox=rounded(14,new Color(c.getRed(),c.getGreen(),c.getBlue(),35)); iconBox.setPreferredSize(new Dimension(48,48)); iconBox.setLayout(new GridBagLayout()); iconBox.add(text(icon,22,Font.BOLD,c)); p.add(iconBox,BorderLayout.WEST);
        JPanel tx=new JPanel(); tx.setOpaque(false); tx.setLayout(new BoxLayout(tx,BoxLayout.Y_AXIS)); tx.add(text(title,12,Font.BOLD,MUTED)); tx.add(Box.createVerticalStrut(6)); tx.add(text(value,24,Font.BOLD,TEXT)); tx.add(Box.createVerticalStrut(3)); tx.add(text(sub,10,Font.PLAIN,MUTED)); p.add(tx,BorderLayout.CENTER); return p;
    }

    // NEW: This-month card showing both income and expense
    static JPanel monthCard(double income, double expense){
        JPanel p=card(); p.setLayout(new BorderLayout(14,0)); p.setBorder(new EmptyBorder(16,18,16,18));
        Color blue=new Color(14,165,233);
        JPanel iconBox=rounded(14,new Color(blue.getRed(),blue.getGreen(),blue.getBlue(),35)); 
        iconBox.setPreferredSize(new Dimension(48,48)); 
        iconBox.setLayout(new GridBagLayout()); 
        iconBox.add(text("◷",22,Font.BOLD,blue)); 
        p.add(iconBox,BorderLayout.WEST);
        JPanel tx=new JPanel(); tx.setOpaque(false); tx.setLayout(new BoxLayout(tx,BoxLayout.Y_AXIS)); 
        tx.add(text(t("This month","इस महीने"),12,Font.BOLD,MUTED)); 
        tx.add(Box.createVerticalStrut(5));
        
        JPanel inRow=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); inRow.setOpaque(false);
        inRow.add(text("＋",13,Font.BOLD,GREEN));
        inRow.add(text(money(income),14,Font.BOLD,GREEN));
        tx.add(inRow);
        
        JPanel exRow=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); exRow.setOpaque(false);
        exRow.add(text("−",13,Font.BOLD,RED));
        exRow.add(text(money(expense),14,Font.BOLD,RED));
        tx.add(exRow);
        
        p.add(tx,BorderLayout.CENTER); return p;
    }
    
    static class GradientPanel extends JPanel{ GradientPanel(){setOpaque(false);} protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setPaint(new GradientPaint(0,0,new Color(79,70,229),getWidth(),getHeight(),new Color(168,85,247)));g2.fillRoundRect(0,0,getWidth(),getHeight(),22,22);g2.dispose();super.paintComponent(g);} }
    static JPanel headerRow(JLabel titleLbl,JLabel trailingLbl){ if(narrowMode){ JPanel h=new JPanel(); h.setOpaque(false); h.setLayout(new BoxLayout(h,BoxLayout.Y_AXIS)); titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT); trailingLbl.setAlignmentX(Component.LEFT_ALIGNMENT); h.add(titleLbl); h.add(Box.createVerticalStrut(2)); h.add(trailingLbl); return h; } JPanel h=new JPanel(new BorderLayout()); h.setOpaque(false); h.add(titleLbl,BorderLayout.WEST); h.add(trailingLbl,BorderLayout.EAST); return h; }
    static JPanel monthlyChartCard(){ JPanel p=card(); p.setLayout(new BorderLayout(0,12)); p.add(headerRow(text(t("Cash flow","कैश फ्लो"),14,Font.BOLD,TEXT),text(t("Last 6 months","पिछले 6 महीने"),11,Font.PLAIN,MUTED)),BorderLayout.NORTH); p.add(new CashChart(),BorderLayout.CENTER); return p; }
    static class CashChart extends JPanel{ CashChart(){setOpaque(false);setPreferredSize(new Dimension(0,190));} protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(),h=getHeight(); double max=1; YearMonth now=YearMonth.now(); double[] in=new double[6],out=new double[6]; String[] labels=new String[6]; for(int i=0;i<6;i++){YearMonth ym=now.minusMonths(5-i);labels[i]=ym.getMonth().toString().substring(0,3);for(Tx x:filteredForCurrentUser())if(x.date.startsWith(ym.toString())){if(x.type.equals("Income"))in[i]+=x.amount;else out[i]+=x.amount;}max=Math.max(max,Math.max(in[i],out[i]));} int left=40,base=h-28,top=18;g2.setColor(BORDER);for(int k=0;k<4;k++){int y=top+(base-top)*k/3;g2.drawLine(left,y,w-12,y);}int bw=Math.max(12,(w-left-20)/18);for(int i=0;i<6;i++){int x=left+8+i*(w-left-20)/6;int ih=(int)((base-top-15)*(in[i]/max));int oh=(int)((base-top-15)*(out[i]/max));if(in[i]>0&&ih<3)ih=3;if(out[i]>0&&oh<3)oh=3;g2.setColor(GREEN);g2.fillRoundRect(x,base-ih,bw,ih,7,7);g2.setColor(RED);g2.fillRoundRect(x+bw+4,base-oh,bw,oh,7,7);g2.setColor(MUTED);g2.setFont(font(Font.PLAIN,10));g2.drawString(labels[i],x,base+17);}g2.setColor(GREEN);g2.fillRoundRect(w-130,8,9,9,3,3);g2.setColor(MUTED);g2.drawString("Income",w-116,17);g2.setColor(RED);g2.fillRoundRect(w-62,8,9,9,3,3);g2.setColor(MUTED);g2.drawString("Spend",w-48,17);g2.dispose();} }
    static JPanel categoryChartCard(){
        JPanel p=card(); p.setLayout(new BorderLayout(0,8));
        p.add(headerRow(text(t("Spending by Category","कैटेगरी के अनुसार खर्च"),15,Font.BOLD,TEXT),text(t("This month","इस महीने"),11,Font.PLAIN,MUTED)),BorderLayout.NORTH);
        final String[] names=CATS; final Color[] cs=CAT_COLORS;
        final double[] vals=new double[names.length]; String ym=YearMonth.now().toString();
        for(Tx x:filteredForCurrentUser()) if(x.type.equals("Expense") && x.date.startsWith(ym)){for(int i=0;i<names.length;i++)if(x.category.equals(names[i])){vals[i]+=x.amount;break;}}
        double total=0; for(double v:vals) total+=v;
        JPanel body=new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        DonutChart donut=new DonutChart(vals,total,cs); donut.setAlignmentX(Component.CENTER_ALIGNMENT); body.add(donut);
        JPanel legend=new JPanel(); legend.setOpaque(false); legend.setLayout(new BoxLayout(legend,BoxLayout.Y_AXIS)); legend.setAlignmentX(Component.CENTER_ALIGNMENT);
        int[] pcts=percentagesSummingTo100(vals,total);
        for(int i=0;i<names.length;i++){int pct=pcts[i]; JPanel r=new JPanel(new BorderLayout(7,0));r.setOpaque(false);r.add(text("●",11,Font.BOLD,cs[i]),BorderLayout.WEST);r.add(text(catLabel(names[i]),10,Font.PLAIN,TEXT),BorderLayout.CENTER);r.add(text(pct+"%",10,Font.BOLD,MUTED),BorderLayout.EAST);r.setBorder(new EmptyBorder(3,0,3,0));legend.add(r);} body.add(Box.createVerticalStrut(10)); body.add(legend); p.add(body,BorderLayout.CENTER); return p;
    }
    static int[] percentagesSummingTo100(double[] vals,double total){
        int n=vals.length; int[] out=new int[n]; if(total<=0) return out;
        double[] raw=new double[n]; double[] rem=new double[n]; int used=0;
        for(int i=0;i<n;i++){ raw[i]=vals[i]*100.0/total; out[i]=(int)Math.floor(raw[i]); rem[i]=raw[i]-out[i]; used+=out[i]; }
        int remaining=100-used;
        Integer[] order=new Integer[n]; for(int i=0;i<n;i++) order[i]=i;
        Arrays.sort(order,new Comparator<Integer>(){public int compare(Integer a,Integer b){return Double.compare(rem[b],rem[a]);}});
        for(int k=0;k<remaining && k<n;k++) out[order[k]]++;
        return out;
    }
    static class DonutChart extends JPanel{
        final double[] vals; final double total; final Color[] cs;
        DonutChart(double[] v,double t,Color[] c){vals=v;total=t;cs=c;setOpaque(false);setPreferredSize(new Dimension(250,210));}
        protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int size=Math.min(getWidth(),getHeight())-34;int x=12,y=(getHeight()-size)/2;
            if(total>0){ double angle=90; double cum=0; for(int i=0;i<vals.length;i++){ cum+=vals[i]; double nextAngle=90-(cum*360.0/total); int sweep=(int)Math.round(angle-nextAngle); g2.setColor(cs[i]); if(sweep!=0) g2.fillArc(x,y,size,size,(int)Math.round(angle),-sweep); angle=nextAngle; } }
            else{g2.setColor(MUTED);g2.fillArc(x,y,size,size,0,360);}int hole=size/2;g2.setColor(CARD);g2.fillOval(x+size/4,y+size/4,hole,hole);g2.setColor(TEXT);g2.setFont(font(Font.BOLD,16));String a=money(total);g2.drawString(a,x+size/2-g2.getFontMetrics().stringWidth(a)/2,y+size/2+2);g2.setColor(MUTED);g2.setFont(font(Font.PLAIN,9));String b=t("Total spent","कुल खर्च");g2.drawString(b,x+size/2-g2.getFontMetrics().stringWidth(b)/2,y+size/2+18);g2.dispose();}
    }
    static JPanel recentCard(){ JPanel p=card();p.setLayout(new BorderLayout(0,10));p.add(text(t("Recent transactions","हाल के लेन-देन"),14,Font.BOLD,TEXT),BorderLayout.NORTH);JPanel list=new JPanel();list.setOpaque(false);list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));List<Tx> a=filteredForCurrentUser();Collections.sort(a,new Comparator<Tx>(){public int compare(Tx x,Tx y){ int byDate=y.date.compareTo(x.date); if(byDate!=0)return byDate; return Long.compare(y.id,x.id); }});int n=Math.min(5,a.size());for(int i=0;i<n;i++){Tx x=a.get(i);JPanel r=new JPanel(new BorderLayout(10,0));r.setOpaque(false);JLabel dot=text(x.type.equals("Income")?"＋":"−",16,Font.BOLD,x.type.equals("Income")?GREEN:RED);dot.setHorizontalAlignment(SwingConstants.CENTER);dot.setPreferredSize(new Dimension(30,30));dot.setOpaque(true);dot.setBackground(new Color((x.type.equals("Income")?GREEN:RED).getRed(),(x.type.equals("Income")?GREEN:RED).getGreen(),(x.type.equals("Income")?GREEN:RED).getBlue(),20));r.add(dot,BorderLayout.WEST);JPanel d=new JPanel();d.setOpaque(false);d.setLayout(new BoxLayout(d,BoxLayout.Y_AXIS));d.add(text(x.description,12,Font.BOLD,TEXT));d.add(text(catLabel(x.category)+"  •  "+displayDate(x.date),10,Font.PLAIN,MUTED));r.add(d,BorderLayout.CENTER);r.add(text((x.type.equals("Income")?"+":"-")+money(x.amount),12,Font.BOLD,x.type.equals("Income")?GREEN:RED),BorderLayout.EAST);r.setBorder(new EmptyBorder(5,0,5,0));list.add(r);if(i<n-1)list.add(new JSeparator());}if(n==0)list.add(text(t("No transactions yet. Add your first one.","अभी कोई लेन-देन नहीं। पहला जोड़ें।"),12,Font.PLAIN,MUTED));p.add(list,BorderLayout.CENTER);return p; }
    static JPanel budgetCard(){ JPanel p=card();p.setLayout(new BorderLayout(0,10));double b=loadBudget(),spent=monthExpense();p.add(text(t("Monthly budget","मासिक बजट"),14,Font.BOLD,TEXT),BorderLayout.NORTH);JPanel center=new JPanel();center.setOpaque(false);center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));center.add(text(b>0?money(spent)+" / "+money(b):t("No budget set","बजट सेट नहीं है"),20,Font.BOLD,TEXT));center.add(Box.createVerticalStrut(8));JProgressBar pb=new JProgressBar(0,100);pb.setValue(b<=0?0:Math.min(100,(int)Math.round(spent/b*100)));pb.setStringPainted(false);pb.setBorderPainted(false);pb.setBackground(BG);pb.setForeground(b>0 && spent>b?RED:ACCENT);center.add(pb);center.add(Box.createVerticalStrut(6));center.add(text(b<=0?t("Set a budget in Settings.","सेटिंग्स में बजट सेट करें।"):((int)Math.round(spent/b*100))+"% "+t("used this month","इस महीने उपयोग"),11,Font.PLAIN,MUTED));p.add(center,BorderLayout.CENTER);JButton s=button(t("Set budget","बजट सेट करें"),ACCENT,Color.WHITE);s.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){showSettings();}});p.add(s,BorderLayout.SOUTH);return p; }

    static void showAdd(){showAdd("Expense");}
    static void goDashboard(){activeNav="Dashboard";updateNavButtons();rebuild();}

    static void showAdd(String defaultType){
        activeNav="Add";updateNavButtons();
        JPanel p=pageShell(t("Add transaction","लेन-देन जोड़ें"),t("Record income or an expense in a few seconds.","आय या खर्च कुछ सेकंड में दर्ज करें।"));
        JPanel wrap=new JPanel(new GridBagLayout());wrap.setOpaque(false);JPanel form=card();form.setPreferredSize(new Dimension(760,430));form.setLayout(new GridBagLayout());
        GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(7,8,7,8);g.fill=GridBagConstraints.HORIZONTAL;g.weightx=1;int r=0;
        final JComboBox<String> type=new JComboBox<String>(new String[]{"Expense","Income"});type.setSelectedItem(defaultType);localizeTypeCombo(type);final JTextField amount=new JTextField();final JTextField date=new JTextField(LocalDate.now().format(DF));
        final JComboBox<String> cat=new JComboBox<String>(CATS);localizeCategoryCombo(cat);final JTextField desc=new JTextField();final JCheckBox recurring=new JCheckBox(t("Make this a recurring monthly transaction","इसे मासिक आवर्ती लेन-देन बनाएं"));recurring.setOpaque(false);recurring.setForeground(TEXT);recurring.setFont(font(Font.PLAIN,12));
        addField(form,g,r++,t("Transaction type","लेन-देन प्रकार"),type);addField(form,g,r++,t("Amount","राशि")+" ("+currency+")",amount);
        g.gridx=0;g.gridy=r;g.gridwidth=1;g.weightx=.3;form.add(text(t("Date (yyyy-mm-dd)","तारीख (yyyy-mm-dd)"),12,Font.BOLD,TEXT),g);g.gridx=1;g.weightx=.7;form.add(datePickerRow(date),g);r++;
        final JLabel catLabel=text(t("Category","कैटेगरी"),12,Font.BOLD,TEXT), descLabel=text(t("Description","विवरण"),12,Font.BOLD,TEXT);
        g.gridx=0;g.gridy=r;g.gridwidth=1;g.weightx=.3;form.add(catLabel,g);g.gridx=1;g.weightx=.7;styleInput(cat);form.add(cat,g);r++;
        g.gridx=0;g.gridy=r;g.weightx=.3;form.add(descLabel,g);g.gridx=1;g.weightx=.7;styleInput(desc);form.add(desc,g);r++;
        g.gridx=1;g.gridy=r++;g.weightx=1;form.add(recurring,g);
        g.gridx=0;g.gridy=r;g.gridwidth=2;JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setOpaque(false);
        JButton cancel=button(t("Cancel","रद्द करें"),BG,TEXT);cancel.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){goDashboard();}});
        JButton saveBtn=button(t("Save transaction","सेव करें"),ACCENT,Color.WHITE);
        ActionListener saveAction=new ActionListener(){public void actionPerformed(ActionEvent e){
            try{
                double a=Double.parseDouble(amount.getText().trim());
                if(a<=0)throw new Exception();
                LocalDate d=LocalDate.parse(date.getText().trim(),DF);
                if(d.isAfter(LocalDate.now())){ JOptionPane.showMessageDialog(frame,t("Future dates are not allowed.","भविष्य की तारीख मान्य नहीं है।"),"ExpenseWise",JOptionPane.ERROR_MESSAGE); return; }
                String ty=type.getSelectedItem().toString();
                String category=ty.equals("Expense")?String.valueOf(cat.getSelectedItem()):"Income";
                txs.add(new Tx(nextId(),user,ty,d.format(DF),category,ty.equals("Expense")?(desc.getText().trim().length()==0?"Untitled":desc.getText().trim()):"Income",a,ty.equals("Expense")&&recurring.isSelected()));
                save();goDashboard();
            }catch(Exception ex){JOptionPane.showMessageDialog(frame,t("Enter a valid positive amount and date.","सही राशि और तारीख दर्ज करें."),"ExpenseWise",JOptionPane.ERROR_MESSAGE);}
        }};
        saveBtn.addActionListener(saveAction);
        amount.addActionListener(saveAction);
        desc.addActionListener(saveAction);
        date.addActionListener(saveAction);
        actions.add(cancel);actions.add(saveBtn);form.add(actions,g);wrap.add(form);p.add(wrap,BorderLayout.CENTER);setContent(p);
        type.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){boolean expense="Expense".equals(type.getSelectedItem());catLabel.setVisible(expense);cat.setVisible(expense);descLabel.setVisible(expense);desc.setVisible(expense);recurring.setVisible(expense);form.revalidate();form.repaint();}});
        boolean expense="Expense".equals(defaultType);catLabel.setVisible(expense);cat.setVisible(expense);descLabel.setVisible(expense);desc.setVisible(expense);recurring.setVisible(expense);form.revalidate();
        if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(saveBtn);
    }
    static void addField(JPanel p,GridBagConstraints g,int row,String label,JComponent comp){g.gridx=0;g.gridy=row;g.gridwidth=1;g.weightx=.3;p.add(text(label,12,Font.BOLD,TEXT),g);g.gridx=1;g.weightx=.7;styleInput(comp);p.add(comp,g);}
    static void styleInput(final JComponent c){
        c.setFont(font(Font.PLAIN,12)); c.setForeground(TEXT); c.setBackground(INPUT_BG);
        c.setBorder(new FocusBorder(8));
        Object prev=c.getClientProperty("styleInputListener");
        if(prev instanceof FocusListener) c.removeFocusListener((FocusListener)prev);
        FocusAdapter fl=new FocusAdapter(){ public void focusGained(FocusEvent e){ c.repaint(); } public void focusLost(FocusEvent e){ c.repaint(); } };
        c.putClientProperty("styleInputListener",fl);
        c.addFocusListener(fl);
    }

    static JPanel datePickerRow(final JTextField field){
        styleInput(field);
        final JButton cal=new JButton("\uD83D\uDCC5");
        cal.setFocusPainted(false);cal.setFont(font(Font.PLAIN,13));
        cal.setBackground(CARD);cal.setForeground(TEXT);
        cal.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(7,10,7,10)));
        cal.setToolTipText(t("Pick a date","तारीख चुनें"));
        cal.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){showCalendarPopup(field,cal);}});
        JPanel wrap=new JPanel(new BorderLayout(6,0));wrap.setOpaque(false);
        wrap.add(field,BorderLayout.CENTER);wrap.add(cal,BorderLayout.EAST);
        return wrap;
    }
    static void showCalendarPopup(final JTextField field,final Component anchor){
        LocalDate base;
        try{ base=LocalDate.parse(field.getText().trim(),DF); }catch(Exception ex){ base=LocalDate.now(); }
        final LocalDate[] cur={ base };
        final boolean[] updating={false};
        Window parentWindow = SwingUtilities.getWindowAncestor(anchor);
        if(parentWindow==null) parentWindow = frame;
        final JDialog dlg = parentWindow instanceof Dialog ? new JDialog((Dialog)parentWindow) : new JDialog(parentWindow);
        dlg.setUndecorated(true);dlg.setModal(false);
        JPanel panel=card();panel.setLayout(new BorderLayout(0,10));
        panel.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(12,12,12,12)));
        JButton prev=button("<",BG,TEXT);
        final JButton next=button(">",BG,TEXT);
        prev.setPreferredSize(new Dimension(30,28));next.setPreferredSize(new Dimension(30,28));
        final JComboBox<String> monthCombo=new JComboBox<String>(new String[]{"January","February","March","April","May","June","July","August","September","October","November","December"});
        monthCombo.setFont(font(Font.BOLD,12));monthCombo.setBackground(CARD);monthCombo.setForeground(TEXT);monthCombo.setPreferredSize(new Dimension(112,26));
        int nowYear=LocalDate.now().getYear();
        Integer[] yearOptions=new Integer[101];
        for(int yi=0;yi<yearOptions.length;yi++) yearOptions[yi]=nowYear-100+yi;
        final JComboBox<Integer> yearCombo=new JComboBox<Integer>(yearOptions);
        yearCombo.setFont(font(Font.BOLD,12));yearCombo.setBackground(CARD);yearCombo.setForeground(TEXT);yearCombo.setPreferredSize(new Dimension(78,26));
        JPanel midSel=new JPanel(new FlowLayout(FlowLayout.CENTER,6,0));midSel.setOpaque(false);
        midSel.add(monthCombo);midSel.add(yearCombo);
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.add(prev,BorderLayout.WEST);header.add(midSel,BorderLayout.CENTER);header.add(next,BorderLayout.EAST);
        panel.add(header,BorderLayout.NORTH);
        final JPanel grid=new JPanel(new GridLayout(0,7,4,4));grid.setOpaque(false);grid.setPreferredSize(new Dimension(230,190));
        panel.add(grid,BorderLayout.CENTER);
        final Runnable[] refresh=new Runnable[1];
        refresh[0]=new Runnable(){public void run(){
            YearMonth ym=YearMonth.from(cur[0]);
            YearMonth currentYM=YearMonth.from(LocalDate.now());
            updating[0]=true;
            monthCombo.setSelectedIndex(ym.getMonthValue()-1);
            yearCombo.setSelectedItem(ym.getYear());
            updating[0]=false;
            boolean canGoNext = ym.isBefore(currentYM);
            next.setEnabled(canGoNext);
            next.setForeground(canGoNext?TEXT:new Color(MUTED.getRed(),MUTED.getGreen(),MUTED.getBlue(),90));
            next.setBackground(canGoNext?BG:new Color(BG.getRed(),BG.getGreen(),BG.getBlue(),120));
            grid.removeAll();
            String[] dow={"Su","Mo","Tu","We","Th","Fr","Sa"};
            for(String d:dow){JLabel dl=text(d,10,Font.BOLD,MUTED);dl.setHorizontalAlignment(SwingConstants.CENTER);grid.add(dl);}
            LocalDate first=ym.atDay(1);
            int lead=first.getDayOfWeek().getValue()%7;
            for(int i=0;i<lead;i++)grid.add(new JLabel(""));
            LocalDate todayNow=LocalDate.now();
            for(int d=1;d<=ym.lengthOfMonth();d++){
                final LocalDate day=ym.atDay(d);
                boolean isToday=day.equals(todayNow);
                boolean isSel=day.format(DF).equals(field.getText().trim());
                boolean isFuture=day.isAfter(todayNow);
                JButton db=new JButton(String.valueOf(d));
                db.setFocusPainted(false);db.setFont(font(Font.PLAIN,11));db.setMargin(new Insets(2,2,2,2));
                if(isFuture){
                    db.setEnabled(false);
                    db.setBackground(CARD);
                    db.setForeground(new Color(MUTED.getRed(),MUTED.getGreen(),MUTED.getBlue(),80));
                    db.setBorder(new LineBorder(new Color(BORDER.getRed(),BORDER.getGreen(),BORDER.getBlue(),80),1,true));
                } else {
                    db.setBackground(isSel?ACCENT:(isToday?new Color(ACCENT.getRed(),ACCENT.getGreen(),ACCENT.getBlue(),40):CARD));
                    db.setForeground(isSel?Color.WHITE:TEXT);
                    db.setBorder(new LineBorder(BORDER,1,true));
                    db.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){field.setText(day.format(DF));dlg.dispose();}});
                }
                grid.add(db);
            }
            grid.revalidate();grid.repaint();
        }};
        prev.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){cur[0]=cur[0].minusMonths(1);refresh[0].run();}});
        next.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            if(!next.isEnabled()) return;
            cur[0]=cur[0].plusMonths(1);refresh[0].run();
        }});
        monthCombo.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            if(updating[0])return;
            int newMonth=monthCombo.getSelectedIndex()+1;
            int newYear=cur[0].getYear();
            YearMonth candidate=YearMonth.of(newYear,newMonth);
            if(candidate.isAfter(YearMonth.from(LocalDate.now()))){
                updating[0]=true;
                monthCombo.setSelectedIndex(LocalDate.now().getMonthValue()-1);
                updating[0]=false;
                return;
            }
            int day=Math.min(cur[0].getDayOfMonth(),candidate.lengthOfMonth());
            cur[0]=candidate.atDay(day);
            refresh[0].run();
        }});
        yearCombo.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            if(updating[0])return;
            Integer sel=(Integer)yearCombo.getSelectedItem();
            if(sel==null)return;
            int newYear=sel;
            int newMonth=cur[0].getMonthValue();
            if(newYear==LocalDate.now().getYear() && newMonth>LocalDate.now().getMonthValue()){
                newMonth=LocalDate.now().getMonthValue();
                updating[0]=true;
                monthCombo.setSelectedIndex(newMonth-1);
                updating[0]=false;
            }
            int day=Math.min(cur[0].getDayOfMonth(),YearMonth.of(newYear,newMonth).lengthOfMonth());
            cur[0]=LocalDate.of(newYear,newMonth,day);
            refresh[0].run();
        }});
        JButton todayBtn=button(t("Today","आज"),BG,ACCENT);
        todayBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){field.setText(LocalDate.now().format(DF));dlg.dispose();}});
        JButton clearBtn=button(t("Clear","साफ करें"),BG,MUTED);
        clearBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){field.setText("");dlg.dispose();}});
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.CENTER,8,0));footer.setOpaque(false);footer.add(todayBtn);footer.add(clearBtn);
        panel.add(footer,BorderLayout.SOUTH);
        refresh[0].run();
        dlg.setContentPane(panel);dlg.pack();
        try{ Point loc=anchor.getLocationOnScreen(); dlg.setLocation(loc.x-160,loc.y+anchor.getHeight()+4); }catch(Exception ignored){}
        dlg.addWindowFocusListener(new WindowFocusListener(){ public void windowGainedFocus(WindowEvent e){} public void windowLostFocus(WindowEvent e){dlg.dispose();} });
        dlg.setVisible(true);
    }

    static void showTransactions(){
        JPanel p=pageShell(t("Transactions","लेन-देन"),t("Search by date, month, description, category and type.","तारीख, महीना, विवरण, कैटेगरी और प्रकार से खोजें।"));
        JPanel body=new JPanel(new BorderLayout(0,12));body.setOpaque(false);body.add(filterBar(),BorderLayout.NORTH);
        JPanel tableCard=card();tableCard.setLayout(new BorderLayout(0,8));
        model=new DefaultTableModel(new Object[]{t("Serial No.","क्रमांक"),t("Date","तारीख"),t("Description","विवरण"),t("Category","कैटेगरी"),t("Type","प्रकार"),t("Amount","राशि"),t("Recurring","आवर्ती")},0){public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(model);table.setRowHeight(40);table.setFont(font(Font.PLAIN,12));table.setForeground(TEXT);table.setBackground(CARD);table.setGridColor(BORDER);table.setShowVerticalLines(false);table.setIntercellSpacing(new Dimension(0,0));
        table.getTableHeader().setFont(font(Font.BOLD,11));table.getTableHeader().setForeground(MUTED);table.getTableHeader().setBackground(INPUT_BG);table.getTableHeader().setPreferredSize(new Dimension(0,38));
        table.getColumnModel().getColumn(0).setPreferredWidth(70);table.getColumnModel().getColumn(5).setPreferredWidth(120);
        DefaultTableCellRenderer centered=new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable tb,Object v,boolean sel,boolean foc,int row,int col){
                Component c=super.getTableCellRendererComponent(tb,v,sel,foc,row,col);
                setHorizontalAlignment(SwingConstants.CENTER);
                c.setBackground(sel?new Color(ACCENT.getRed(),ACCENT.getGreen(),ACCENT.getBlue(),28):(row%2==0?CARD:INPUT_BG));
                setForeground(TEXT);
                return c;
            }
        };
        for(int ci=0;ci<table.getColumnCount();ci++)table.getColumnModel().getColumn(ci).setCellRenderer(centered);
        table.getColumnModel().getColumn(3).setCellRenderer(new ChipCellRenderer(){public Color colorFor(String v){return catColorByLabel(v);}});
        table.getColumnModel().getColumn(4).setCellRenderer(new ChipCellRenderer(){public Color colorFor(String v){return v.equals(t("Income","आय"))?GREEN:RED;}});
        table.getColumnModel().getColumn(6).setCellRenderer(new ChipCellRenderer(){public Color colorFor(String v){return v.equals(t("Yes","हाँ"))?SECOND:MUTED;}});
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable tb,Object v,boolean sel,boolean foc,int row,int col){
                Component c=super.getTableCellRendererComponent(tb,v,sel,foc,row,col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(font(Font.BOLD,12));
                boolean income=String.valueOf(tb.getModel().getValueAt(row,4)).equals(t("Income","आय"));
                setForeground(income?GREEN:RED);
                c.setBackground(sel?new Color(ACCENT.getRed(),ACCENT.getGreen(),ACCENT.getBlue(),28):(row%2==0?CARD:INPUT_BG));
                return c;
            }
        });
        TableCellRenderer hr=table.getTableHeader().getDefaultRenderer();if(hr instanceof DefaultTableCellRenderer)((DefaultTableCellRenderer)hr).setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane sp=new JScrollPane(table);sp.setBorder(new LineBorder(BORDER,1,true));sp.getViewport().setBackground(CARD);
        tableCard.add(sp,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setOpaque(false);
        final JButton undo=button(t("Undo delete","पूर्ववत करें"),BG,ACCENT);
        undo.setVisible(lastDeletedTx!=null);
        undo.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            if(lastDeletedTx!=null){ txs.add(lastDeletedTx); lastDeletedTx=null; save(); refreshTable(); undo.setVisible(false); }
        }});
        undoDeleteBtn=undo;
        JButton edit=button(t("Edit","संपादित करें"),ACCENT,Color.WHITE);edit.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){editSelected();}});
        JButton del=button(t("Delete","हटाएं"),new Color(254,226,226),RED);del.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){deleteSelected();}});
        actions.add(undo);actions.add(edit);actions.add(del);tableCard.add(actions,BorderLayout.SOUTH);
        body.add(tableCard,BorderLayout.CENTER);p.add(body,BorderLayout.CENTER);setContent(p);refreshTable();
    }
    static abstract class ChipCellRenderer implements TableCellRenderer{
        public abstract Color colorFor(String value);
        public Component getTableCellRendererComponent(JTable tb,Object value,boolean sel,boolean foc,int row,int col){ String v=String.valueOf(value); Color c=colorFor(v); JPanel wrap=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0)); wrap.setOpaque(true); wrap.setBackground(sel?new Color(ACCENT.getRed(),ACCENT.getGreen(),ACCENT.getBlue(),28):(row%2==0?CARD:INPUT_BG)); JLabel l=pill(v,c); wrap.add(l); return wrap; }
    }
    static JPanel filterBar(){JPanel f=card();f.setLayout(new BorderLayout(0,10));
        searchField=new PlaceholderField(t("Search by description...","विवरण से खोजें..."));fromField=new PlaceholderField(t("From (yyyy-mm-dd)","से (yyyy-mm-dd)"));toField=new PlaceholderField(t("To (yyyy-mm-dd)","तक (yyyy-mm-dd)"));catFilter=new JComboBox<String>(concat(new String[]{t("All categories","सभी कैटेगरी")},CATS));localizeCategoryCombo(catFilter);monthFilter=new JComboBox<String>(months());typeFilter=new JComboBox<String>(new String[]{t("All types","सभी प्रकार"),"Expense","Income"});localizeTypeCombo(typeFilter);for(JComponent c:new JComponent[]{searchField,fromField,toField,catFilter,monthFilter,typeFilter})styleInput(c);
        searchField.setHorizontalAlignment(JTextField.LEFT); fromField.setHorizontalAlignment(JTextField.CENTER); toField.setHorizontalAlignment(JTextField.CENTER);
        Object prev=searchField.getClientProperty("styleInputListener"); if(prev instanceof FocusListener) searchField.removeFocusListener((FocusListener)prev);
        searchField.setBorder(new EmptyBorder(8,4,8,10));searchField.setOpaque(false);
        JLabel searchIcon=text("\uD83D\uDD0D",13,Font.PLAIN,MUTED); searchIcon.setBorder(new EmptyBorder(0,12,0,6));
        JPanel searchWrap=new JPanel(new BorderLayout(0,0)); searchWrap.setOpaque(true); searchWrap.setBackground(INPUT_BG);
        searchWrap.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(0,0,0,0)));
        searchWrap.add(searchIcon,BorderLayout.WEST); searchWrap.add(searchField,BorderLayout.CENTER);
        searchField.addFocusListener(new FocusAdapter(){ public void focusGained(FocusEvent e){ searchWrap.setBorder(new CompoundBorder(new LineBorder(ACCENT,2,true),new EmptyBorder(0,0,0,0))); } public void focusLost(FocusEvent e){ searchWrap.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(0,0,0,0))); } });
        JPanel row1=new JPanel(new GridBagLayout()); row1.setOpaque(false);
        GridBagConstraints g1=new GridBagConstraints(); g1.insets=new Insets(0,0,0,8); g1.fill=GridBagConstraints.HORIZONTAL; g1.weightx=1;
        g1.gridx=0; row1.add(searchWrap,g1);
        JButton clear=button(t("Clear","साफ करें"),BG,TEXT);clear.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){searchField.setText("");fromField.setText("");toField.setText("");catFilter.setSelectedIndex(0);monthFilter.setSelectedIndex(0);typeFilter.setSelectedIndex(0);refreshTable();}});
        g1.gridx=1; g1.weightx=0; g1.insets=new Insets(0,0,0,0); row1.add(clear,g1);
        JPanel row2=new JPanel(new GridBagLayout()); row2.setOpaque(false);
        GridBagConstraints g2=new GridBagConstraints(); g2.insets=new Insets(0,0,0,8); g2.fill=GridBagConstraints.HORIZONTAL;
        g2.gridx=0;g2.weightx=1.3;row2.add(datePickerRow(fromField),g2); g2.gridx=1;g2.weightx=1.3;row2.add(datePickerRow(toField),g2); g2.gridx=2;g2.weightx=1;row2.add(catFilter,g2); g2.gridx=3;g2.weightx=0.85;row2.add(monthFilter,g2); g2.gridx=4;g2.weightx=0.85;g2.insets=new Insets(0,0,0,0);row2.add(typeFilter,g2);
        f.add(row1,BorderLayout.NORTH); f.add(row2,BorderLayout.SOUTH);
        DocumentListener dl=new DocumentListener(){public void insertUpdate(DocumentEvent e){refreshTable();}public void removeUpdate(DocumentEvent e){refreshTable();}public void changedUpdate(DocumentEvent e){refreshTable();}};searchField.getDocument().addDocumentListener(dl);fromField.getDocument().addDocumentListener(dl);toField.getDocument().addDocumentListener(dl);catFilter.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){refreshTable();}});monthFilter.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){refreshTable();}});typeFilter.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){refreshTable();}});return f;}

    static void refreshTable(){
        if(model==null)return;
        model.setRowCount(0);
        List<Tx> list=filtered();
        currentDisplayTxs=list;
        int sn=1;
        for(Tx x:list){
            model.addRow(new Object[]{sn++, displayDate(x.date), x.description, catLabel(x.category), typeLabel(x.type), money(x.amount), x.recurring?t("Yes","हाँ"):t("No","नहीं")});
        }
        if(status!=null)status.setText(list.size()+" "+t("transactions","लेन-देन"));
    }
    static List<Tx> filtered(){List<Tx> out=new ArrayList<Tx>();String q=searchField==null?"":searchField.getText().trim().toLowerCase(Locale.ENGLISH);String fr=fromField==null?"":fromField.getText().trim();String to=toField==null?"":toField.getText().trim();String cat=catFilter==null?"":String.valueOf(catFilter.getSelectedItem());String mon=monthFilter==null?"":String.valueOf(monthFilter.getSelectedItem());String typ=typeFilter==null?"":String.valueOf(typeFilter.getSelectedItem());for(Tx x:txs){if(!x.user.equals(user))continue;if(q.length()>0&&!x.description.toLowerCase(Locale.ENGLISH).contains(q))continue;if(fr.length()>0&&!x.date.startsWith(fr)&&x.date.compareTo(fr)<0)continue;if(to.length()>0&&!x.date.startsWith(to)&&x.date.compareTo(to)>0)continue;if(cat.length()>0&&!cat.equals(t("All categories","सभी कैटेगरी"))&&!x.category.equals(cat))continue;if(mon.length()>0&&!mon.equals(t("All months","सभी महीने"))&&!x.date.startsWith(mon))continue;if(typ.length()>0&&!typ.equals(t("All types","सभी प्रकार"))&&!x.type.equals(typ))continue;out.add(x);}
        Collections.sort(out,new Comparator<Tx>(){public int compare(Tx a,Tx b){return Long.compare(b.id,a.id);}});
        return out;
    }
    static void editSelected(){
        if(table==null||table.getSelectedRow()<0){JOptionPane.showMessageDialog(frame,t("Select a transaction first.","पहले लेन-देन चुनें।"));return;}
        int row=table.getSelectedRow();
        if(row<0 || row>=currentDisplayTxs.size())return;
        final Tx x=currentDisplayTxs.get(row);
        final JDialog d=new JDialog(frame,t("Edit transaction","लेन-देन संपादित करें"),true);
        d.setSize(420,430);d.setLocationRelativeTo(frame);d.getContentPane().setBackground(CARD);d.setResizable(false);
        JPanel p=new JPanel();p.setBackground(CARD);p.setBorder(new EmptyBorder(20,20,20,20));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.add(text(t("Edit transaction","लेन-देन संपादित करें"),16,Font.BOLD,TEXT));p.add(Box.createVerticalStrut(14));
        p.add(text(t("Transaction type","लेन-देन प्रकार"),11,Font.BOLD,MUTED));
        final JComboBox<String> type=new JComboBox<String>(new String[]{"Expense","Income"});type.setSelectedItem(x.type);localizeTypeCombo(type);styleInput(type);type.setAlignmentX(Component.LEFT_ALIGNMENT);type.setMaximumSize(new Dimension(2000,38));
        p.add(type);p.add(Box.createVerticalStrut(10));
        p.add(text(t("Date","तारीख"),11,Font.BOLD,MUTED));
        final JTextField date=new JTextField(x.date);date.setAlignmentX(Component.LEFT_ALIGNMENT);date.setMaximumSize(new Dimension(2000,40));
        JPanel dateRow=datePickerRow(date);dateRow.setAlignmentX(Component.LEFT_ALIGNMENT);dateRow.setMaximumSize(new Dimension(2000,42));
        p.add(dateRow);p.add(Box.createVerticalStrut(10));
        p.add(text(t("Description","विवरण"),11,Font.BOLD,MUTED));
        final JTextField desc=new JTextField(x.description);styleInput(desc);desc.setAlignmentX(Component.LEFT_ALIGNMENT);desc.setMaximumSize(new Dimension(2000,38));
        p.add(desc);p.add(Box.createVerticalStrut(10));
        final JLabel catLbl=text(t("Category","कैटेगरी"),11,Font.BOLD,MUTED);
        p.add(catLbl);
        final JComboBox<String> cat=new JComboBox<String>(CATS);localizeCategoryCombo(cat);
        if(Arrays.asList(CATS).contains(x.category)) cat.setSelectedItem(x.category);
        styleInput(cat);cat.setAlignmentX(Component.LEFT_ALIGNMENT);cat.setMaximumSize(new Dimension(2000,38));
        p.add(cat);p.add(Box.createVerticalStrut(10));
        p.add(text(t("Amount","राशि"),11,Font.BOLD,MUTED));
        final JTextField amount=new JTextField(String.valueOf(x.amount));styleInput(amount);amount.setAlignmentX(Component.LEFT_ALIGNMENT);amount.setMaximumSize(new Dimension(2000,38));
        p.add(amount);p.add(Box.createVerticalStrut(8));
        final JCheckBox rec=new JCheckBox(t("Recurring","आवर्ती"));rec.setOpaque(false);rec.setForeground(TEXT);rec.setFont(font(Font.PLAIN,12));rec.setSelected(x.recurring);rec.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rec);p.add(Box.createVerticalStrut(12));
        final JLabel err=text("",11,Font.PLAIN,RED);err.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(err);p.add(Box.createVerticalStrut(6));
        JPanel btns=new JPanel(new GridLayout(1,2,10,0));btns.setOpaque(false);btns.setAlignmentX(Component.LEFT_ALIGNMENT);btns.setMaximumSize(new Dimension(2000,44));
        JButton cancel=button(t("Cancel","रद्द करें"),BG,TEXT);cancel.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){d.dispose();}});
        JButton ok=button(t("Save","सेव करें"),ACCENT,Color.WHITE);
        Runnable toggleCat=new Runnable(){public void run(){ boolean expense="Expense".equals(type.getSelectedItem()); catLbl.setVisible(expense); cat.setVisible(expense); rec.setVisible(expense); p.revalidate(); p.repaint(); }};
        type.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){toggleCat.run();}});
        toggleCat.run();
        ActionListener saveAct=new ActionListener(){public void actionPerformed(ActionEvent e){
            try{
                String ty=String.valueOf(type.getSelectedItem());
                LocalDate dt=LocalDate.parse(date.getText().trim(),DF);
                if(dt.isAfter(LocalDate.now())){ err.setText(t("Future dates are not allowed.","भविष्य की तारीख मान्य नहीं है।")); return; }
                x.date=dt.format(DF);
                x.description=desc.getText().trim();
                x.type=ty;
                x.category=ty.equals("Expense")?String.valueOf(cat.getSelectedItem()):"Income";
                x.amount=Double.parseDouble(amount.getText().trim());
                x.recurring=ty.equals("Expense")&&rec.isSelected();
                save();refreshTable();d.dispose();
            }catch(Exception ex){err.setText(t("Invalid values.","अमान्य मान।"));}
        }};
        ok.addActionListener(saveAct);
        amount.addActionListener(saveAct);
        desc.addActionListener(saveAct);
        date.addActionListener(saveAct);
        btns.add(cancel);btns.add(ok);p.add(btns);
        d.getRootPane().setDefaultButton(ok);
        d.setContentPane(p);d.setVisible(true);
    }
    static void deleteSelected(){
        if(table==null||table.getSelectedRow()<0){JOptionPane.showMessageDialog(frame,t("Select a transaction first.","पहले लेन-देन चुनें।"));return;}
        int row=table.getSelectedRow();
        if(row<0 || row>=currentDisplayTxs.size())return;
        Tx x=currentDisplayTxs.get(row);
        int ok=JOptionPane.showConfirmDialog(frame,t("Delete selected transaction?","चयनित लेन-देन हटाएं?"),"ExpenseWise",JOptionPane.YES_NO_OPTION);
        if(ok==JOptionPane.YES_OPTION){
            txs.remove(x);save();lastDeletedTx=x;refreshTable();
            if(undoDeleteBtn!=null)undoDeleteBtn.setVisible(true);
        }
    }

    static void showRecurring(){
        JPanel p=pageShell(t("Recurring expenses","आवर्ती खर्च"),t("Monthly recurring transactions are generated automatically.","मासिक आवर्ती लेन-देन अपने आप बनाए जाते हैं।"));
        JPanel c=card();c.setLayout(new BorderLayout(0,12));
        c.add(text(t("Active recurring rules / generated transactions","सक्रिय आवर्ती नियम / जनरेट किए गए लेन-देन"),14,Font.BOLD,TEXT),BorderLayout.NORTH);
        JPanel list=new JPanel();list.setOpaque(false);list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
        final List<Tx> recList=new ArrayList<Tx>();
        for(Tx x:filteredForCurrentUser())if(x.recurring)recList.add(x);
        Collections.sort(recList,new Comparator<Tx>(){public int compare(Tx a,Tx b){return Long.compare(b.id,a.id);}});
        if(recList.isEmpty()){
            list.add(text(t("No recurring transactions yet. Tick the recurring option while adding a transaction.","अभी कोई आवर्ती लेन-देन नहीं है। जोड़ते समय recurring विकल्प चुनें।"),12,Font.PLAIN,MUTED));
        } else {
            for(final Tx x:recList){
                JPanel r=new JPanel(new BorderLayout(10,0));r.setOpaque(false);
                r.add(pill(t("MONTHLY","मासिक"),ACCENT),BorderLayout.WEST);
                JPanel d=new JPanel();d.setOpaque(false);d.setLayout(new BoxLayout(d,BoxLayout.Y_AXIS));
                d.add(text(x.description,12,Font.BOLD,TEXT));
                d.add(text(catLabel(x.category)+"  •  "+displayDate(x.date),10,Font.PLAIN,MUTED));
                r.add(d,BorderLayout.CENTER);
                JPanel rightP=new JPanel(new BorderLayout(8,0));rightP.setOpaque(false);
                rightP.add(text((x.type.equals("Income")?"+":"-")+money(x.amount),12,Font.BOLD,x.type.equals("Income")?GREEN:RED),BorderLayout.CENTER);
                JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));btns.setOpaque(false);
                JButton edit=button(t("Edit","संपादित करें"),BG,TEXT);edit.setFont(font(Font.BOLD,11));
                edit.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){editRecurring(x,list);}});
                JButton del=button(t("Delete","हटाएं"),new Color(254,226,226),RED);del.setFont(font(Font.BOLD,11));
                del.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
                    int ok=JOptionPane.showConfirmDialog(frame,t("Delete this recurring transaction? This also removes future generated entries with the same details.","यह आवर्ती लेन-देन हटाएं? भविष्य की समान एंट्रियां भी हटेंगी।"),"ExpenseWise",JOptionPane.YES_NO_OPTION);
                    if(ok!=JOptionPane.YES_OPTION)return;
                    List<Tx> rm=new ArrayList<Tx>();
                    for(Tx t:txs){
                        if(!t.user.equals(user)||!t.recurring)continue;
                        if(t.type.equals(x.type) && t.category.equals(x.category) && t.description.equals(x.description) && Math.abs(t.amount-x.amount)<0.001) rm.add(t);
                    }
                    txs.removeAll(rm);save();rebuild();
                }});
                btns.add(edit);btns.add(del);
                rightP.add(btns,BorderLayout.EAST);
                r.add(rightP,BorderLayout.EAST);
                r.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER),new EmptyBorder(12,2,12,12)));
                list.add(r);
            }
        }
        JScrollPane sp=vScroll(list);c.add(sp,BorderLayout.CENTER);p.add(c,BorderLayout.CENTER);setContent(p);
    }
    static void editRecurring(final Tx x,final JPanel list){
        final JDialog d=new JDialog(frame,t("Edit recurring","आवर्ती संपादित करें"),true);
        d.setSize(400,400);d.setLocationRelativeTo(frame);d.getContentPane().setBackground(CARD);d.setResizable(false);
        JPanel p=new JPanel();p.setBackground(CARD);p.setBorder(new EmptyBorder(20,20,20,20));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.add(text(t("Edit recurring rule","आवर्ती नियम संपादित करें"),15,Font.BOLD,TEXT));p.add(Box.createVerticalStrut(12));
        p.add(text(t("Start date","प्रारंभ तारीख"),11,Font.BOLD,MUTED));
        final JTextField date=new JTextField(x.date);date.setAlignmentX(Component.LEFT_ALIGNMENT);date.setMaximumSize(new Dimension(2000,40));
        JPanel dateRow=datePickerRow(date);dateRow.setAlignmentX(Component.LEFT_ALIGNMENT);dateRow.setMaximumSize(new Dimension(2000,42));
        p.add(dateRow);p.add(Box.createVerticalStrut(10));
        p.add(text(t("Description","विवरण"),11,Font.BOLD,MUTED));
        final JTextField desc=new JTextField(x.description);styleInput(desc);desc.setAlignmentX(Component.LEFT_ALIGNMENT);desc.setMaximumSize(new Dimension(2000,38));
        p.add(desc);p.add(Box.createVerticalStrut(10));
        p.add(text(t("Amount","राशि"),11,Font.BOLD,MUTED));
        final JTextField amt=new JTextField(String.valueOf(x.amount));styleInput(amt);amt.setAlignmentX(Component.LEFT_ALIGNMENT);amt.setMaximumSize(new Dimension(2000,38));
        p.add(amt);p.add(Box.createVerticalStrut(16));
        final JLabel err=text("",11,Font.PLAIN,RED);err.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(err);p.add(Box.createVerticalStrut(6));
        JPanel btns=new JPanel(new GridLayout(1,2,10,0));btns.setOpaque(false);btns.setAlignmentX(Component.LEFT_ALIGNMENT);btns.setMaximumSize(new Dimension(2000,42));
        JButton cancel=button(t("Cancel","रद्द करें"),BG,TEXT);cancel.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){d.dispose();}});
        JButton ok=button(t("Save","सेव करें"),ACCENT,Color.WHITE);
        ActionListener saveAct=new ActionListener(){public void actionPerformed(ActionEvent e){
            try{
                LocalDate dt=LocalDate.parse(date.getText().trim(),DF);
                if(dt.isAfter(LocalDate.now())){ err.setText(t("Future dates are not allowed.","भविष्य की तारीख मान्य नहीं है।")); return; }
                x.date=dt.format(DF);
                x.description=desc.getText().trim();
                x.amount=Double.parseDouble(amt.getText().trim());
                save();d.dispose();rebuild();
            }catch(Exception ex){err.setText(t("Invalid values.","अमान्य मान।"));}
        }};
        ok.addActionListener(saveAct);
        date.addActionListener(saveAct);
        desc.addActionListener(saveAct);
        amt.addActionListener(saveAct);
        btns.add(cancel);btns.add(ok);p.add(btns);
        d.getRootPane().setDefaultButton(ok);
        d.setContentPane(p);d.setVisible(true);
    }

    static JTextField rptFrom, rptTo; static JComboBox<String> rptType, rptCat; static JPanel rptListPanel; static JLabel rptCount;
    static final Map<Long,JCheckBox> rptChecks=new LinkedHashMap<Long,JCheckBox>();
    static List<Tx> rptFilteredBase(){
        List<Tx> out=new ArrayList<Tx>();
        String fr=rptFrom==null?"":rptFrom.getText().trim(), to=rptTo==null?"":rptTo.getText().trim();
        String typ=rptType==null?"":String.valueOf(rptType.getSelectedItem()), cat=rptCat==null?"":String.valueOf(rptCat.getSelectedItem());
        for(Tx x:filteredForCurrentUser()){
            if(fr.length()>0&&!x.date.startsWith(fr)&&x.date.compareTo(fr)<0)continue;
            if(to.length()>0&&!x.date.startsWith(to)&&x.date.compareTo(to)>0)continue;
            if(typ.length()>0&&!typ.equals(t("All types","सभी प्रकार"))&&!x.type.equals(typ))continue;
            if(cat.length()>0&&!cat.equals(t("All categories","सभी कैटेगरी"))&&!x.category.equals(cat))continue;
            out.add(x);
        }
        Collections.sort(out,new Comparator<Tx>(){public int compare(Tx a,Tx b){return Long.compare(b.id,a.id);}});
        return out;
    }
    static void updateRptCount(){ if(rptCount==null)return; int total=rptChecks.size(),sel=0; for(JCheckBox cb:rptChecks.values())if(cb.isSelected())sel++; rptCount.setText(t("Selected: ","चयनित: ")+sel+" / "+total+t(" transactions"," लेन-देन")); }
    static void rebuildRptList(){
        if(rptListPanel==null)return;
        rptListPanel.removeAll(); rptChecks.clear();
        List<Tx> list=rptFilteredBase();
        if(list.isEmpty()){ rptListPanel.add(text(t("No transactions match these filters.","इन फ़िल्टर से कोई लेन-देन नहीं मिला।"),12,Font.PLAIN,MUTED)); }
        else{
            for(final Tx x:list){
                JPanel row=new JPanel(new BorderLayout(10,0)); row.setOpaque(false); row.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER),new EmptyBorder(8,2,8,2)));
                final JCheckBox cb=new JCheckBox(); cb.setOpaque(false); cb.setSelected(true); cb.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){updateRptCount();}});
                rptChecks.put(x.id,cb); row.add(cb,BorderLayout.WEST);
                JPanel d=new JPanel(); d.setOpaque(false); d.setLayout(new BoxLayout(d,BoxLayout.Y_AXIS)); d.add(text(x.description,12,Font.BOLD,TEXT)); d.add(text(catLabel(x.category)+"  •  "+displayDate(x.date),10,Font.PLAIN,MUTED)); row.add(d,BorderLayout.CENTER);
                row.add(text((x.type.equals("Income")?"+":"-")+money(x.amount),12,Font.BOLD,x.type.equals("Income")?GREEN:RED),BorderLayout.EAST);
                rptListPanel.add(row);
            }
        }
        rptListPanel.revalidate(); rptListPanel.repaint(); updateRptCount();
    }
    static List<Tx> rptSelected(){
        List<Tx> out=new ArrayList<Tx>();
        for(Map.Entry<Long,JCheckBox> e:rptChecks.entrySet())if(e.getValue().isSelected()){Tx x=find(e.getKey());if(x!=null)out.add(x);}
        Collections.sort(out,new Comparator<Tx>(){public int compare(Tx a,Tx b){return Long.compare(b.id,a.id);}});
        return out;
    }
    static void showReports(){
        JPanel p=pageShell(t("Reports & export","रिपोर्ट व एक्सपोर्ट"),t("Choose a date range and pick exactly which transactions to export.","एक तारीख सीमा चुनें और तय करें कि कौन-से लेन-देन एक्सपोर्ट करने हैं।"));
        JPanel body=new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        JPanel filterCard=card(); filterCard.setLayout(new GridBagLayout());
        GridBagConstraints g=new GridBagConstraints(); g.insets=new Insets(4,5,4,5); g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1;
        rptFrom=new PlaceholderField(t("From date (yyyy-mm-dd)","से तारीख (yyyy-mm-dd)")); rptTo=new PlaceholderField(t("To date (yyyy-mm-dd)","तक तारीख (yyyy-mm-dd)"));
        rptType=new JComboBox<String>(new String[]{t("All types","सभी प्रकार"),"Expense","Income"}); localizeTypeCombo(rptType); rptCat=new JComboBox<String>(concat(new String[]{t("All categories","सभी कैटेगरी")},CATS)); localizeCategoryCombo(rptCat);
        for(JComponent c:new JComponent[]{rptFrom,rptTo,rptType,rptCat})styleInput(c);
        rptFrom.setHorizontalAlignment(JTextField.CENTER); rptTo.setHorizontalAlignment(JTextField.CENTER);
        g.gridx=0;g.gridy=0;filterCard.add(datePickerRow(rptFrom),g); g.gridx=1;filterCard.add(datePickerRow(rptTo),g); g.gridx=2;filterCard.add(rptType,g); g.gridx=3;filterCard.add(rptCat,g);
        JButton rptClear=button(t("Clear filters","फ़िल्टर साफ करें"),BG,TEXT); rptClear.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){rptFrom.setText("");rptTo.setText("");rptType.setSelectedIndex(0);rptCat.setSelectedIndex(0);rebuildRptList();}});
        g.gridx=4;g.weightx=0;filterCard.add(rptClear,g);
        DocumentListener rdl=new DocumentListener(){public void insertUpdate(DocumentEvent e){rebuildRptList();}public void removeUpdate(DocumentEvent e){rebuildRptList();}public void changedUpdate(DocumentEvent e){rebuildRptList();}};
        rptFrom.getDocument().addDocumentListener(rdl); rptTo.getDocument().addDocumentListener(rdl);
        rptType.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){rebuildRptList();}}); rptCat.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){rebuildRptList();}});
        body.add(filterCard); body.add(Box.createVerticalStrut(14));
        JPanel listCard=card(); listCard.setLayout(new BorderLayout(0,10));
        JPanel listHeader=new JPanel(new BorderLayout()); listHeader.setOpaque(false);
        listHeader.add(sectionHeader(t("Select transactions to export","एक्सपोर्ट के लिए लेन-देन चुनें")),BorderLayout.WEST);
        JPanel selBtns=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); selBtns.setOpaque(false);
        JButton selAll=button(t("Select all","सभी चुनें"),BG,TEXT); selAll.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){for(JCheckBox cb:rptChecks.values())cb.setSelected(true);updateRptCount();}});
        JButton selNone=button(t("Clear selection","चयन हटाएं"),BG,TEXT); selNone.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){for(JCheckBox cb:rptChecks.values())cb.setSelected(false);updateRptCount();}});
        selBtns.add(selAll); selBtns.add(selNone); listHeader.add(selBtns,BorderLayout.EAST); listCard.add(listHeader,BorderLayout.NORTH);
        rptListPanel=new JPanel(); rptListPanel.setOpaque(false); rptListPanel.setLayout(new BoxLayout(rptListPanel,BoxLayout.Y_AXIS));
        JScrollPane lsp=vScroll(rptListPanel); lsp.setPreferredSize(new Dimension(0,240));
        listCard.add(lsp,BorderLayout.CENTER);
        rptCount=text("",11,Font.PLAIN,MUTED); listCard.add(rptCount,BorderLayout.SOUTH);
        body.add(listCard); body.add(Box.createVerticalStrut(14));
        JPanel exportCard=card(); exportCard.setLayout(new GridBagLayout());
        GridBagConstraints g2=new GridBagConstraints(); g2.insets=new Insets(6,6,6,6); g2.fill=GridBagConstraints.HORIZONTAL; g2.weightx=1;
        g2.gridx=0;g2.gridy=0;g2.gridwidth=3;exportCard.add(sectionHeader(t("Export center","एक्सपोर्ट सेंटर")),g2); g2.gridy++;g2.gridwidth=1;
        JButton ex=button("▣  "+t("Export Excel (.xlsx)","Excel एक्सपोर्ट करें (.xlsx)"),new Color(20,170,125),Color.WHITE);
        ex.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){List<Tx> sel=rptSelected();if(sel.isEmpty()){JOptionPane.showMessageDialog(frame,t("Select at least one transaction to export.","एक्सपोर्ट के लिए कम से कम एक लेन-देन चुनें।"));return;}exportExcel(sel);}});
        JButton pdf=button("▤  "+t("Export PDF (.pdf)","PDF एक्सपोर्ट करें (.pdf)"),ACCENT,Color.WHITE);
        pdf.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){List<Tx> sel=rptSelected();if(sel.isEmpty()){JOptionPane.showMessageDialog(frame,t("Select at least one transaction to export.","एक्सपोर्ट के लिए कम से कम एक लेन-देन चुनें।"));return;}exportPdf(sel);}});
        JButton csv=button("▦  "+t("Export CSV (.csv)","CSV एक्सपोर्ट करें (.csv)"),SECOND,Color.WHITE);
        csv.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){List<Tx> sel=rptSelected();if(sel.isEmpty()){JOptionPane.showMessageDialog(frame,t("Select at least one transaction to export.","एक्सपोर्ट के लिए कम से कम एक लेन-देन चुनें।"));return;}exportCsv(sel);}});
        g2.gridx=0;exportCard.add(ex,g2); g2.gridx=1; exportCard.add(pdf,g2); g2.gridx=2; exportCard.add(csv,g2);
        body.add(exportCard);
        JScrollPane pageScroll=vScroll(body);
        p.add(pageScroll,BorderLayout.CENTER); setContent(p); rebuildRptList();
    }

    static void showSettings(){
        JPanel p=pageShell(t("Settings","सेटिंग्स"),t("Customize your workspace and personal finance preferences.","अपने वर्कस्पेस और वित्तीय प्राथमिकताएं कस्टमाइज़ करें।"));
        JPanel body=new JPanel();body.setOpaque(false);body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));

        JPanel appearance=card();appearance.setLayout(new BorderLayout(20,0));
        JPanel a=new JPanel();a.setOpaque(false);a.setLayout(new BoxLayout(a,BoxLayout.Y_AXIS));
        a.add(text(t("Appearance","दिखावट"),15,Font.BOLD,TEXT));a.add(Box.createVerticalStrut(4));
        a.add(text(t("Choose the theme you like. It is saved automatically.","अपनी पसंद की थीम चुनें। यह अपने आप सेव होगी।"),11,Font.PLAIN,MUTED));
        appearance.add(a,BorderLayout.WEST);
        JPanel choices=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));choices.setOpaque(false);
        JButton light=button("☀ "+t("Light","लाइट"),theme==Theme.LIGHT?ACCENT:BG,theme==Theme.LIGHT?Color.WHITE:TEXT);
        JButton dark=button("◐ "+t("Dark","डार्क"),theme==Theme.DARK?ACCENT:BG,theme==Theme.DARK?Color.WHITE:TEXT);
        JButton grad=button("◈ "+t("Gradient","ग्रेडिएंट"),theme==Theme.GRADIENT?ACCENT:BG,theme==Theme.GRADIENT?Color.WHITE:TEXT);
        light.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){theme=Theme.LIGHT;savePrefs();setPalette();rebuild();}});
        dark.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){theme=Theme.DARK;savePrefs();setPalette();rebuild();}});
        grad.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){theme=Theme.GRADIENT;savePrefs();setPalette();rebuild();}});
        choices.add(light);choices.add(dark);choices.add(grad);appearance.add(choices,BorderLayout.EAST);
        body.add(appearance);body.add(Box.createVerticalStrut(14));

        JPanel langCard=card();langCard.setLayout(new BorderLayout(20,0));
        JPanel lc=new JPanel();lc.setOpaque(false);lc.setLayout(new BoxLayout(lc,BoxLayout.Y_AXIS));
        lc.add(text(t("Language","भाषा"),15,Font.BOLD,TEXT));lc.add(Box.createVerticalStrut(4));
        lc.add(text(t("Select your preferred language.","अपनी पसंदीदा भाषा चुनें।"),11,Font.PLAIN,MUTED));
        langCard.add(lc,BorderLayout.WEST);
        final JComboBox<String> langCombo=new JComboBox<String>(LANGS);langCombo.setSelectedItem(lang);styleCombo(langCombo);langCombo.setPreferredSize(new Dimension(180,36));
        langCombo.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){lang=langCombo.getSelectedItem().toString();savePrefs();rebuild();}});
        JPanel langWrap=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));langWrap.setOpaque(false);langWrap.add(langCombo);
        langCard.add(langWrap,BorderLayout.EAST);
        body.add(langCard);body.add(Box.createVerticalStrut(14));

        JPanel currCard=card();currCard.setLayout(new BorderLayout(20,0));
        JPanel cc=new JPanel();cc.setOpaque(false);cc.setLayout(new BoxLayout(cc,BoxLayout.Y_AXIS));
        cc.add(text(t("Currency","मुद्रा"),15,Font.BOLD,TEXT));cc.add(Box.createVerticalStrut(4));
        cc.add(text(t("Choose the currency symbol used across the app.","पूरे ऐप में उपयोग होने वाला मुद्रा चिन्ह चुनें।"),11,Font.PLAIN,MUTED));
        currCard.add(cc,BorderLayout.WEST);
        final JComboBox<String> currCombo=new JComboBox<String>(CURRENCIES);currCombo.setSelectedItem(currency);styleCombo(currCombo);currCombo.setPreferredSize(new Dimension(220,36));
        currCombo.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean isSelected,boolean cellHasFocus){
            Component c=super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
            if(value!=null){ String sym=String.valueOf(value); String name=CURRENCY_NAMES.get(sym); setText(name!=null?(sym+"  "+name):sym); }
            return c;
        }});
        currCombo.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){currency=String.valueOf(currCombo.getSelectedItem());savePrefs();rebuild();}});
        JPanel currWrap=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));currWrap.setOpaque(false);currWrap.add(currCombo);
        currCard.add(currWrap,BorderLayout.EAST);
        body.add(currCard);body.add(Box.createVerticalStrut(14));

        JPanel budget=card();budget.setLayout(new BorderLayout(15,0));
        JPanel bt=new JPanel();bt.setOpaque(false);bt.setLayout(new BoxLayout(bt,BoxLayout.Y_AXIS));
        bt.add(text(t("Monthly budget","मासिक बजट"),15,Font.BOLD,TEXT));
        bt.add(text(t("Set a spending limit for the current user.","वर्तमान यूज़र के लिए खर्च की सीमा सेट करें।"),11,Font.PLAIN,MUTED));
        double curBudget=loadBudget();bt.add(Box.createVerticalStrut(6));
        bt.add(text(curBudget>0?t("Current budget: ","वर्तमान बजट: ")+money(curBudget):t("No budget set yet.","अभी कोई बजट सेट नहीं है।"),12,Font.BOLD,curBudget>0?ACCENT:MUTED));
        budget.add(bt,BorderLayout.CENTER);
        JButton addBudget=button(t("Add Budget","बजट जोड़ें"),ACCENT,Color.WHITE);addBudget.setPreferredSize(new Dimension(150,42));
        addBudget.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){showAddBudgetDialog();}});
        JPanel budgetBtnWrap=new JPanel(new GridBagLayout());budgetBtnWrap.setOpaque(false);budgetBtnWrap.add(addBudget);
        budget.add(budgetBtnWrap,BorderLayout.EAST);
        body.add(budget);body.add(Box.createVerticalStrut(14));

        JPanel resetCard=card();resetCard.setLayout(new BorderLayout(15,0));
        JPanel rt=new JPanel();rt.setOpaque(false);rt.setLayout(new BoxLayout(rt,BoxLayout.Y_AXIS));
        rt.add(text(t("Reset data","डेटा रीसेट करें"),15,Font.BOLD,TEXT));rt.add(Box.createVerticalStrut(4));
        rt.add(text(t("Clear only Income, only Expense, or just your budget — pick whichever one you want to reset.","सिर्फ़ आय, सिर्फ़ खर्च, या सिर्फ़ अपना बजट — जो चाहें वही रीसेट करें।"),11,Font.PLAIN,MUTED));
        resetCard.add(rt,BorderLayout.CENTER);
        JPanel resetBtns=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));resetBtns.setOpaque(false);
        JButton resetBudgetBtn=button(t("Reset Budget","बजट रीसेट करें"),BG,TEXT);resetBudgetBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){int ok=JOptionPane.showConfirmDialog(frame,t("Reset your budget back to 0?","अपना बजट वापस 0 पर सेट करें?"),"ExpenseWise",JOptionPane.YES_NO_OPTION);if(ok==JOptionPane.YES_OPTION){saveBudget(0);rebuild();}}});
        JButton clearIncomeBtn=button(t("Clear Income","आय साफ़ करें"),new Color(220,252,231),GREEN);clearIncomeBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){int ok=JOptionPane.showConfirmDialog(frame,t("Delete ALL of your Income entries and reset income to 0? This can't be undone.","आपकी सभी आय की एंट्री हटाकर आय को 0 कर दें? यह वापस नहीं होगा।"),"ExpenseWise",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(ok==JOptionPane.YES_OPTION){List<Tx> rm=new ArrayList<Tx>();for(Tx x:filteredForCurrentUser())if(x.type.equals("Income"))rm.add(x);txs.removeAll(rm);save();rebuild();}}});
        JButton clearExpenseBtn=button(t("Clear Expense","खर्च साफ़ करें"),new Color(254,226,226),RED);clearExpenseBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){int ok=JOptionPane.showConfirmDialog(frame,t("Delete ALL of your Expense entries and reset expense to 0? This can't be undone.","आपकी सभी खर्च की एंट्री हटाकर खर्च को 0 कर दें? यह वापस नहीं होगा।"),"ExpenseWise",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);if(ok==JOptionPane.YES_OPTION){List<Tx> rm=new ArrayList<Tx>();for(Tx x:filteredForCurrentUser())if(x.type.equals("Expense"))rm.add(x);txs.removeAll(rm);save();rebuild();}}});
        resetBtns.add(resetBudgetBtn);resetBtns.add(clearIncomeBtn);resetBtns.add(clearExpenseBtn);
        resetCard.add(resetBtns,BorderLayout.EAST);
        body.add(resetCard);body.add(Box.createVerticalStrut(14));

        JPanel usersCard=card();usersCard.setLayout(new BorderLayout(0,12));
        usersCard.add(text(t("User profile","यूज़र प्रोफाइल"),15,Font.BOLD,TEXT),BorderLayout.NORTH);
        JPanel r=new JPanel(new BorderLayout(10,0));r.setOpaque(false);
        JPanel avatarWrap=new JPanel(new GridBagLayout());avatarWrap.setOpaque(false);avatarWrap.add(personAvatar(40,new Color(118,102,255),user));
        r.add(avatarWrap,BorderLayout.WEST);
        JPanel d=new JPanel();d.setOpaque(false);d.setLayout(new BoxLayout(d,BoxLayout.Y_AXIS));
        d.add(text(user,13,Font.BOLD,TEXT));
        String em=userEmail.containsKey(user)?userEmail.get(user):"",ph=userPhone.containsKey(user)?userPhone.get(user):"";
        d.add(text((em.length()>0?em:t("No email","कोई ईमेल नहीं"))+"  •  "+(ph.length()>0?ph:t("No phone","कोई फ़ोन नहीं")),11,Font.PLAIN,MUTED));
        d.setBorder(new EmptyBorder(0,10,0,0));
        r.add(d,BorderLayout.CENTER);
        JButton uedit=button(t("Edit my profile","मेरी प्रोफ़ाइल संपादित करें"),ACCENT,Color.WHITE);
        uedit.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){editUserDialog(user);}});
        r.add(uedit,BorderLayout.EAST);
        usersCard.add(r,BorderLayout.CENTER);
        body.add(usersCard);body.add(Box.createVerticalStrut(14));

        JPanel dangerCard=card();dangerCard.setLayout(new BorderLayout(15,0));
        JPanel dt=new JPanel();dt.setOpaque(false);dt.setLayout(new BoxLayout(dt,BoxLayout.Y_AXIS));
        dt.add(text(t("Delete account","खाता हटाएं"),15,Font.BOLD,RED));dt.add(Box.createVerticalStrut(4));
        dt.add(text(t("Permanently delete your account and all of its transactions, budget and photo. This can't be undone.","अपना खाता और उसके सभी लेन-देन, बजट व फोटो स्थायी रूप से हटाएं। यह वापस नहीं होगा।"),11,Font.PLAIN,MUTED));
        dangerCard.add(dt,BorderLayout.CENTER);
        JButton delAcc=button(t("Delete my account","मेरा खाता हटाएं"),new Color(254,226,226),RED);
        delAcc.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            if(users.size()<=1){ JOptionPane.showMessageDialog(frame,t("You can't delete the only account. Create another account first.","आप एकमात्र खाता नहीं हटा सकते। पहले एक और खाता बनाएं।"),"ExpenseWise",JOptionPane.WARNING_MESSAGE); return; }
            String warn=t("Delete your account \"","अपना खाता \"")+user+t("\" and ALL of its data? This can't be undone.","\" और उसका सारा डेटा हटाएं? यह वापस नहीं होगा।");
            int ok=JOptionPane.showConfirmDialog(frame,warn,"ExpenseWise",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
            if(ok!=JOptionPane.YES_OPTION)return;
            deleteAccount(user);
        }});
        JPanel dangerBtnWrap=new JPanel(new GridBagLayout());dangerBtnWrap.setOpaque(false);dangerBtnWrap.add(delAcc);
        dangerCard.add(dangerBtnWrap,BorderLayout.EAST);
        body.add(dangerCard);body.add(Box.createVerticalStrut(14));

        JPanel about=card();about.setLayout(new BorderLayout());
        about.add(text("ExpenseWise  •  "+t("Offline personal finance tracker","ऑफ़लाइन पर्सनल फाइनेंस ट्रैकर"),12,Font.PLAIN,MUTED),BorderLayout.WEST);
        body.add(about);
        JScrollPane pageScroll=vScroll(body);
        p.add(pageScroll,BorderLayout.CENTER);setContent(p);
    }

    static void editUserDialog(final String oldName){
        final JDialog d=new JDialog(frame,t("Edit profile","प्रोफ़ाइल संपादित करें"),true);
        d.setSize(400,530);d.setLocationRelativeTo(frame);d.getContentPane().setBackground(CARD);d.setResizable(false);
        final int CW=320;
        JPanel p=new JPanel();
        p.setBackground(CARD);
        p.setBorder(new EmptyBorder(20,24,20,24));
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        JLabel titleLbl=text(t("Edit profile","प्रोफ़ाइल संपादित करें"),16,Font.BOLD,TEXT);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        titleLbl.setPreferredSize(new Dimension(CW,24));
        titleLbl.setMaximumSize(new Dimension(CW,24));
        p.add(titleLbl); p.add(Box.createVerticalStrut(14));
        JLabel picLbl=text(t("Profile picture (max 100 KB)","प्रोफ़ाइल फोटो (अधिकतम 100 KB)"),11,Font.BOLD,MUTED);
        picLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        picLbl.setHorizontalAlignment(SwingConstants.LEFT);
        picLbl.setPreferredSize(new Dimension(CW,16));
        picLbl.setMaximumSize(new Dimension(CW,16));
        p.add(picLbl); p.add(Box.createVerticalStrut(8));
        final JPanel avatarHolder=new JPanel(new GridBagLayout());avatarHolder.setOpaque(false);
        final JComponent[] avatarRef={personAvatar(42,new Color(118,102,255),oldName)};
        avatarHolder.add(avatarRef[0]);
        JButton choosePhotoBtn=button(t("Choose photo","फोटो चुनें"),BG,TEXT);
        JButton removePhotoBtn=button(t("Remove","हटाएं"),BG,TEXT);
        JPanel photoRow=new JPanel(new FlowLayout(FlowLayout.CENTER,8,0));
        photoRow.setOpaque(false);
        photoRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        photoRow.setPreferredSize(new Dimension(CW,48));
        photoRow.setMaximumSize(new Dimension(CW,48));
        photoRow.add(avatarHolder);
        photoRow.add(choosePhotoBtn);
        photoRow.add(removePhotoBtn);
        p.add(photoRow); p.add(Box.createVerticalStrut(14));
        Runnable refreshAvatar=new Runnable(){public void run(){ avatarHolder.remove(avatarRef[0]);avatarRef[0]=personAvatar(42,new Color(118,102,255),oldName);avatarHolder.add(avatarRef[0]); avatarHolder.revalidate();avatarHolder.repaint(); }};
        final Runnable refreshAvatarF=refreshAvatar;
        choosePhotoBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){choosePhoto(oldName,refreshAvatarF);}});
        removePhotoBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){removePhoto(oldName);refreshAvatarF.run();}});
        JLabel nameLbl=text(t("Name","नाम"),11,Font.BOLD,MUTED);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLbl.setHorizontalAlignment(SwingConstants.LEFT);
        nameLbl.setPreferredSize(new Dimension(CW,16));
        nameLbl.setMaximumSize(new Dimension(CW,16));
        p.add(nameLbl); p.add(Box.createVerticalStrut(4));
        final JTextField fn=new JTextField(oldName); styleInput(fn);
        fn.setAlignmentX(Component.CENTER_ALIGNMENT);
        fn.setPreferredSize(new Dimension(CW,36));
        fn.setMaximumSize(new Dimension(CW,36));
        p.add(fn); p.add(Box.createVerticalStrut(10));
        JLabel emailLbl=text(t("Email","ईमेल"),11,Font.BOLD,MUTED);
        emailLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailLbl.setHorizontalAlignment(SwingConstants.LEFT);
        emailLbl.setPreferredSize(new Dimension(CW,16));
        emailLbl.setMaximumSize(new Dimension(CW,16));
        p.add(emailLbl); p.add(Box.createVerticalStrut(4));
        final JTextField fe=new JTextField(userEmail.containsKey(oldName)?userEmail.get(oldName):""); styleInput(fe);
        fe.setAlignmentX(Component.CENTER_ALIGNMENT);
        fe.setPreferredSize(new Dimension(CW,36));
        fe.setMaximumSize(new Dimension(CW,36));
        p.add(fe); p.add(Box.createVerticalStrut(10));
        JLabel phoneLbl=text(t("Phone","फ़ोन"),11,Font.BOLD,MUTED);
        phoneLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        phoneLbl.setHorizontalAlignment(SwingConstants.LEFT);
        phoneLbl.setPreferredSize(new Dimension(CW,16));
        phoneLbl.setMaximumSize(new Dimension(CW,16));
        p.add(phoneLbl); p.add(Box.createVerticalStrut(4));
        final JTextField fp=new JTextField(userPhone.containsKey(oldName)?userPhone.get(oldName):""); styleInput(fp);
        fp.setAlignmentX(Component.CENTER_ALIGNMENT);
        fp.setPreferredSize(new Dimension(CW,36));
        fp.setMaximumSize(new Dimension(CW,36));
        p.add(fp); p.add(Box.createVerticalStrut(10));
        JLabel pwLbl=text(t("New password (leave blank to keep current)","नया पासवर्ड (न बदलना हो तो खाली छोड़ें)"),11,Font.BOLD,MUTED);
        pwLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        pwLbl.setHorizontalAlignment(SwingConstants.LEFT);
        pwLbl.setPreferredSize(new Dimension(CW,16));
        pwLbl.setMaximumSize(new Dimension(CW,16));
        p.add(pwLbl); p.add(Box.createVerticalStrut(4));
        final JPasswordField fpw=new JPasswordField(); styleInput(fpw);
        fpw.setAlignmentX(Component.CENTER_ALIGNMENT);
        fpw.setPreferredSize(new Dimension(CW,36));
        fpw.setMaximumSize(new Dimension(CW,36));
        p.add(fpw); p.add(Box.createVerticalStrut(12));
        final JLabel err=text(" ",11,Font.PLAIN,RED);
        err.setAlignmentX(Component.CENTER_ALIGNMENT);
        err.setHorizontalAlignment(SwingConstants.CENTER);
        err.setPreferredSize(new Dimension(CW,16));
        err.setMaximumSize(new Dimension(CW,16));
        p.add(err); p.add(Box.createVerticalStrut(6));
        JButton cancel=button(t("Cancel","रद्द करें"),BG,TEXT);
        cancel.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){d.dispose();}});
        JButton saveBtn=button(t("Save changes","बदलाव सेव करें"),ACCENT,Color.WHITE);
        saveBtn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
            String nn=fn.getText().trim();
            if(nn.length()==0){err.setText(t("Enter a name.","नाम दर्ज करें।"));return;}
            if(!nn.equals(oldName)&&users.contains(nn)){err.setText(t("That name is already in use.","यह नाम पहले से उपयोग में है।"));return;}
            if(!nn.equals(oldName)){
                int idx=users.indexOf(oldName);if(idx>=0)users.set(idx,nn);
                for(Tx x:txs)if(x.user.equals(oldName))x.user=nn;
                String em0=userEmail.remove(oldName);if(em0!=null)userEmail.put(nn,em0);
                String ph0=userPhone.remove(oldName);if(ph0!=null)userPhone.put(nn,ph0);
                String pw0=userPassword.remove(oldName);if(pw0!=null)userPassword.put(nn,pw0);
                String pic0=userPhoto.remove(oldName);if(pic0!=null)userPhoto.put(nn,pic0);
                photoCache.remove(oldName);photoCache.remove(nn);
                Iterator<String> it=circleCache.keySet().iterator();
                while(it.hasNext()){ String k=it.next(); if(k.startsWith(oldName+"@")||k.startsWith(nn+"@")) it.remove(); }
                if(user.equals(oldName))user=nn;
            }
            userEmail.put(nn,fe.getText().trim());userPhone.put(nn,fp.getText().trim());
            String newPw=new String(fpw.getPassword()).trim();
            if(newPw.length()>0)userPassword.put(nn,hashPassword(newPw));
            save();d.dispose();rebuild();JOptionPane.showMessageDialog(frame,t("Profile updated.","प्रोफ़ाइल अपडेट हो गई।"));
        }});
        JPanel btns=new JPanel(new GridLayout(1,2,10,0));
        btns.setOpaque(false);
        btns.setAlignmentX(Component.CENTER_ALIGNMENT);
        btns.setPreferredSize(new Dimension(CW,42));
        btns.setMaximumSize(new Dimension(CW,42));
        btns.add(cancel); btns.add(saveBtn);
        p.add(btns);
        fn.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){saveBtn.doClick();}});
        fe.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){saveBtn.doClick();}});
        fp.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){saveBtn.doClick();}});
        fpw.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){saveBtn.doClick();}});
        d.getRootPane().setDefaultButton(saveBtn);
        d.setContentPane(p);d.setVisible(true);
    }

    // ==================== NEW: SINGLE-CARD LOGIN with inline navigation ====================
    static void showLockScreen(){
        setPalette();applyUiManagerColors();
        sidebarPanel=null; rootPanel=null; content=null;
        JPanel root=new JPanel(new GridBagLayout());root.setBackground(BG);
        final int radius=20;
        JPanel lcard=new JPanel(){
            {setOpaque(false);}
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(),h=getHeight();
                for(int i=6;i>=1;i--){ g2.setColor(new Color(15,23,42,3+i*2)); g2.fillRoundRect(i,i+2,Math.max(1,w-6-i),Math.max(1,h-6-i),radius,radius); }
                g2.setColor(CARD); g2.fillRoundRect(0,0,w-6,h-6,radius,radius);
                g2.dispose(); super.paintComponent(g);
            }
        };
        lcard.setLayout(new BorderLayout());
        lcard.setPreferredSize(new Dimension(440,600));
        lcard.setOpaque(false);

        JPanel header=new JPanel(){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,new Color(79,70,229),getWidth(),getHeight(),new Color(168,85,247)));
                g2.fillRoundRect(0,0,getWidth(),getHeight()+radius,radius,radius);
                g2.dispose(); super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0,130));
        header.setBorder(new EmptyBorder(24,28,20,28));
        header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
        JPanel brandRow=new JPanel(new FlowLayout(FlowLayout.CENTER,8,0)); brandRow.setOpaque(false); brandRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        brandRow.add(text("◆",26,Font.BOLD,Color.WHITE)); brandRow.add(text("ExpenseWise",22,Font.BOLD,Color.WHITE));
        final JLabel tag=text(t("Smart Personal Finance","स्मार्ट पर्सनल फाइनेंस"),11,Font.PLAIN,new Color(255,255,255,205)); tag.setAlignmentX(Component.CENTER_ALIGNMENT);
        final JLabel sub=text(t("Sign in to continue","जारी रखने के लिए साइन इन करें"),11,Font.PLAIN,new Color(255,255,255,180)); sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(Box.createVerticalGlue()); header.add(brandRow); header.add(Box.createVerticalStrut(5)); header.add(tag); header.add(Box.createVerticalStrut(8)); header.add(sub); header.add(Box.createVerticalGlue());
        lcard.add(header,BorderLayout.NORTH);

        final CardLayout cards=new CardLayout();
        final JPanel holder=new JPanel(cards);
        holder.setOpaque(false);
        final String LOGIN="login", SIGNUP="signup", FORGOT="forgot";

        // -------- LOGIN PANEL --------
        JPanel loginPanel=new JPanel(new BorderLayout());
        loginPanel.setOpaque(false);
        loginPanel.setBorder(new EmptyBorder(20,26,22,26));
        JPanel loginForm=new JPanel(new GridBagLayout());
        loginForm.setOpaque(false);
        GridBagConstraints ug=new GridBagConstraints();
        ug.fill=GridBagConstraints.HORIZONTAL; ug.weightx=1; ug.gridx=0; int uy=0;
        ug.gridy=uy++; ug.insets=new Insets(0,0,5,0);
        loginForm.add(text(t("User ID","यूज़र ID"),11,Font.BOLD,TEXT),ug);
        final JTextField uName=new JTextField(); styleInput(uName); uName.setPreferredSize(new Dimension(0,38));
        ug.gridy=uy++; ug.insets=new Insets(0,0,12,0);
        loginForm.add(uName,ug);
        ug.gridy=uy++; ug.insets=new Insets(0,0,5,0);
        loginForm.add(text(t("Password","पासवर्ड"),11,Font.BOLD,TEXT),ug);
        final JPasswordField uPass=new JPasswordField(); styleInput(uPass); uPass.setPreferredSize(new Dimension(0,38));
        ug.gridy=uy++; ug.insets=new Insets(0,0,8,0);
        loginForm.add(uPass,ug);
        final JCheckBox rememberMe=new JCheckBox(t("Remember me","मुझे याद रखें"));
        rememberMe.setOpaque(false); rememberMe.setForeground(TEXT); rememberMe.setFont(font(Font.PLAIN,11));
        rememberMe.setFocusPainted(false);
        final JLabel forgotU=text(t("Forgot password?","पासवर्ड भूल गए?"),11,Font.BOLD,ACCENT);
        forgotU.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel row=new JPanel(new BorderLayout()); row.setOpaque(false);
        row.add(rememberMe,BorderLayout.WEST); row.add(forgotU,BorderLayout.EAST);
        ug.gridy=uy++; ug.insets=new Insets(0,0,10,0);
        loginForm.add(row,ug);
        final JLabel uErr=text(" ",11,Font.PLAIN,RED);
        ug.gridy=uy++; ug.insets=new Insets(0,0,4,0);
        loginForm.add(uErr,ug);
        final JButton uLogin=button(t("Log in","लॉगिन करें"),ACCENT,Color.WHITE);
        uLogin.setFont(font(Font.BOLD,13)); uLogin.setPreferredSize(new Dimension(0,44));
        ug.gridy=uy++; ug.insets=new Insets(0,0,14,0);
        loginForm.add(uLogin,ug);
        JPanel signupRow=new JPanel(new FlowLayout(FlowLayout.CENTER,4,0)); signupRow.setOpaque(false);
        signupRow.add(text(t("New user?","नए यूज़र?"),11,Font.PLAIN,MUTED));
        final JLabel signupLink=text(t("Create account","खाता बनाएं"),11,Font.BOLD,ACCENT);
        signupLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signupRow.add(signupLink);
        ug.gridy=uy++; ug.insets=new Insets(0,0,0,0); ug.weighty=0;
        loginForm.add(signupRow,ug);
        loginPanel.add(loginForm,BorderLayout.NORTH);

        // -------- SIGNUP PANEL --------
        JPanel signupPanel=new JPanel(new BorderLayout());
        signupPanel.setOpaque(false);
        signupPanel.setBorder(new EmptyBorder(20,26,22,26));
        JPanel signupForm=new JPanel(new GridBagLayout());
        signupForm.setOpaque(false);
        GridBagConstraints sg=new GridBagConstraints();
        sg.fill=GridBagConstraints.HORIZONTAL; sg.weightx=1; sg.gridx=0; int sy=0;
        sg.gridy=sy++; sg.insets=new Insets(0,0,5,0);
        signupForm.add(text(t("User ID","यूज़र ID"),11,Font.BOLD,TEXT),sg);
        final JTextField sName=new JTextField(); styleInput(sName); sName.setPreferredSize(new Dimension(0,34));
        sg.gridy=sy++; sg.insets=new Insets(0,0,8,0);
        signupForm.add(sName,sg);
        sg.gridy=sy++; sg.insets=new Insets(0,0,5,0);
        signupForm.add(text(t("Email","ईमेल"),11,Font.BOLD,TEXT),sg);
        final JTextField sEmail=new JTextField(); styleInput(sEmail); sEmail.setPreferredSize(new Dimension(0,34));
        sg.gridy=sy++; sg.insets=new Insets(0,0,8,0);
        signupForm.add(sEmail,sg);
        sg.gridy=sy++; sg.insets=new Insets(0,0,5,0);
        signupForm.add(text(t("Phone","फ़ोन"),11,Font.BOLD,TEXT),sg);
        final JTextField sPhone=new JTextField(); styleInput(sPhone); sPhone.setPreferredSize(new Dimension(0,34));
        sg.gridy=sy++; sg.insets=new Insets(0,0,8,0);
        signupForm.add(sPhone,sg);
        sg.gridy=sy++; sg.insets=new Insets(0,0,5,0);
        signupForm.add(text(t("Password","पासवर्ड"),11,Font.BOLD,TEXT),sg);
        final JPasswordField sPass=new JPasswordField(); styleInput(sPass); sPass.setPreferredSize(new Dimension(0,34));
        sg.gridy=sy++; sg.insets=new Insets(0,0,8,0);
        signupForm.add(sPass,sg);
        sg.gridy=sy++; sg.insets=new Insets(0,0,5,0);
        signupForm.add(text(t("Confirm password","पासवर्ड दोबारा"),11,Font.BOLD,TEXT),sg);
        final JPasswordField sPass2=new JPasswordField(); styleInput(sPass2); sPass2.setPreferredSize(new Dimension(0,34));
        sg.gridy=sy++; sg.insets=new Insets(0,0,8,0);
        signupForm.add(sPass2,sg);
        final JLabel sErr=text(" ",11,Font.PLAIN,RED);
        sg.gridy=sy++; sg.insets=new Insets(0,0,4,0);
        signupForm.add(sErr,sg);
        final JButton sCreate=button(t("Create account","खाता बनाएं"),ACCENT,Color.WHITE);
        sCreate.setFont(font(Font.BOLD,13)); sCreate.setPreferredSize(new Dimension(0,40));
        sg.gridy=sy++; sg.insets=new Insets(0,0,10,0);
        signupForm.add(sCreate,sg);
        JPanel backRow=new JPanel(new FlowLayout(FlowLayout.CENTER,4,0)); backRow.setOpaque(false);
        backRow.add(text(t("Already have an account?","पहले से खाता है?"),11,Font.PLAIN,MUTED));
        final JLabel backLink=text(t("Log in","लॉगिन करें"),11,Font.BOLD,ACCENT);
        backLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backRow.add(backLink);
        sg.gridy=sy++; sg.insets=new Insets(0,0,0,0); sg.weighty=0;
        signupForm.add(backRow,sg);
        signupPanel.add(signupForm,BorderLayout.NORTH);

        // -------- FORGOT PANEL --------
        JPanel forgotPanel=new JPanel(new BorderLayout());
        forgotPanel.setOpaque(false);
        forgotPanel.setBorder(new EmptyBorder(20,26,22,26));
        JPanel forgotForm=new JPanel(new GridBagLayout());
        forgotForm.setOpaque(false);
        GridBagConstraints fg=new GridBagConstraints();
        fg.fill=GridBagConstraints.HORIZONTAL; fg.weightx=1; fg.gridx=0; int fy=0;
        fg.gridy=fy++; fg.insets=new Insets(0,0,5,0);
        forgotForm.add(text(t("Reset password","पासवर्ड रीसेट करें"),15,Font.BOLD,TEXT),fg);
        fg.gridy=fy++; fg.insets=new Insets(0,0,14,0);
        forgotForm.add(text(t("Confirm your user ID and registered email to set a new password.","पासवर्ड बदलने के लिए अपनी यूज़र ID और रजिस्टर्ड ईमेल की पुष्टि करें।"),10,Font.PLAIN,MUTED),fg);
        fg.gridy=fy++; fg.insets=new Insets(0,0,5,0);
        forgotForm.add(text(t("User ID","यूज़र ID"),11,Font.BOLD,TEXT),fg);
        final JTextField fName=new JTextField(); styleInput(fName); fName.setPreferredSize(new Dimension(0,34));
        fg.gridy=fy++; fg.insets=new Insets(0,0,10,0);
        forgotForm.add(fName,fg);
        fg.gridy=fy++; fg.insets=new Insets(0,0,5,0);
        forgotForm.add(text(t("Email","ईमेल"),11,Font.BOLD,TEXT),fg);
        final JTextField fEmail=new JTextField(); styleInput(fEmail); fEmail.setPreferredSize(new Dimension(0,34));
        fg.gridy=fy++; fg.insets=new Insets(0,0,10,0);
        forgotForm.add(fEmail,fg);
        fg.gridy=fy++; fg.insets=new Insets(0,0,5,0);
        forgotForm.add(text(t("New password","नया पासवर्ड"),11,Font.BOLD,TEXT),fg);
        final JPasswordField fPass=new JPasswordField(); styleInput(fPass); fPass.setPreferredSize(new Dimension(0,34));
        fg.gridy=fy++; fg.insets=new Insets(0,0,10,0);
        forgotForm.add(fPass,fg);
        final JLabel fErr=text(" ",11,Font.PLAIN,RED);
        fg.gridy=fy++; fg.insets=new Insets(0,0,4,0);
        forgotForm.add(fErr,fg);
        final JButton fReset=button(t("Reset password","पासवर्ड रीसेट करें"),ACCENT,Color.WHITE);
        fReset.setFont(font(Font.BOLD,13)); fReset.setPreferredSize(new Dimension(0,42));
        fg.gridy=fy++; fg.insets=new Insets(0,0,10,0);
        forgotForm.add(fReset,fg);
        JPanel backRow2=new JPanel(new FlowLayout(FlowLayout.CENTER,4,0)); backRow2.setOpaque(false);
        backRow2.add(text(t("Remember your password?","पासवर्ड याद आ गया?"),11,Font.PLAIN,MUTED));
        final JLabel backLink2=text(t("Log in","लॉगिन करें"),11,Font.BOLD,ACCENT);
        backLink2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backRow2.add(backLink2);
        fg.gridy=fy++; fg.insets=new Insets(0,0,0,0); fg.weighty=0;
        forgotForm.add(backRow2,fg);
        forgotPanel.add(forgotForm,BorderLayout.NORTH);

        holder.add(loginPanel, LOGIN);
        holder.add(signupPanel, SIGNUP);
        holder.add(forgotPanel, FORGOT);
        lcard.add(holder, BorderLayout.CENTER);

        // -------- NAVIGATION ACTIONS --------
        // Switch to signup
        signupLink.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
            sName.setText(uName.getText()); sEmail.setText(""); sPhone.setText(""); sPass.setText(""); sPass2.setText("");
            sErr.setText(" "); cards.show(holder, SIGNUP);
            tag.setText(t("Create a new account","नया खाता बनाएं"));
            sub.setText(t("Fill the details below to sign up.","साइन अप करने के लिए विवरण भरें।"));
            if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(sCreate);
            sName.requestFocusInWindow();
        }});
        // Switch back to login
        backLink.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
            uErr.setText(" "); cards.show(holder, LOGIN);
            tag.setText(t("Smart Personal Finance","स्मार्ट पर्सनल फाइनेंस"));
            sub.setText(t("Sign in to continue","जारी रखने के लिए साइन इन करें"));
            if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(uLogin);
            uName.requestFocusInWindow();
        }});
        // Switch to forgot
        forgotU.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
            String t0=uName.getText().trim();
            fName.setText(t0);
            String em=t0.length()>0 && userEmail.containsKey(t0) ? userEmail.get(t0) : "";
            fEmail.setText(em);
            fPass.setText(""); fErr.setText(" ");
            cards.show(holder, FORGOT);
            tag.setText(t("Reset your password","पासवर्ड रीसेट करें"));
            sub.setText(t("Confirm your details to set a new password.","नया पासवर्ड सेट करने के लिए विवरण की पुष्टि करें।"));
            if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(fReset);
            fName.requestFocusInWindow();
        }});
        // Back to login from forgot
        backLink2.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
            fErr.setText(" "); cards.show(holder, LOGIN);
            tag.setText(t("Smart Personal Finance","स्मार्ट पर्सनल फाइनेंस"));
            sub.setText(t("Sign in to continue","जारी रखने के लिए साइन इन करें"));
            if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(uLogin);
            uName.requestFocusInWindow();
        }});

        String remembered=loadRememberedUser();
        if(remembered.length()>0 && users.contains(remembered)){
            uName.setText(remembered);
            rememberMe.setSelected(true);
        }

        // Login action
        ActionListener loginAct=new ActionListener(){public void actionPerformed(ActionEvent e){
            String target=uName.getText().trim();
            if(target.length()==0){uErr.setText(t("Enter your user ID.","अपनी यूज़र ID दर्ज करें।"));return;}
            if(!users.contains(target)){uErr.setText(t("No such user. Create a new account.","ऐसा यूज़र नहीं है। नया खाता बनाएं।"));return;}
            String stored=userPassword.containsKey(target)?userPassword.get(target):"";
            String entered=new String(uPass.getPassword());
            if(!checkPassword(stored,entered)){uErr.setText(t("Incorrect password.","गलत पासवर्ड।"));return;}
            if(rememberMe.isSelected()) saveRememberedUser(target); else saveRememberedUser("");
            user=target; savePrefs(); rebuild();
        }};
        uLogin.addActionListener(loginAct);
        uPass.addActionListener(loginAct);
        uName.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){uPass.requestFocusInWindow();}});

        // Create action
        ActionListener createAct=new ActionListener(){public void actionPerformed(ActionEvent e){
            String n=sName.getText().trim();
            if(n.length()==0){sErr.setText(t("Enter a user ID.","यूज़र ID दर्ज करें।"));return;}
            if(users.contains(n)){sErr.setText(t("That user ID is already taken.","यह यूज़र ID पहले से उपयोग में है।"));return;}
            String p1=new String(sPass.getPassword()).trim();
            String p2=new String(sPass2.getPassword()).trim();
            if(p1.length()==0){sErr.setText(t("Enter a password.","पासवर्ड दर्ज करें।"));return;}
            if(!p1.equals(p2)){sErr.setText(t("Passwords don't match.","पासवर्ड मेल नहीं खाते।"));return;}
            users.add(n);
            userEmail.put(n,sEmail.getText().trim());
            userPhone.put(n,sPhone.getText().trim());
            userPassword.put(n,hashPassword(p1));
            save();
            uName.setText(n); uPass.setText("");
            uErr.setText(" ");
            JOptionPane.showMessageDialog(frame,t("Account created. Log in now.","खाता बन गया। अब लॉगिन करें।"),"ExpenseWise",JOptionPane.INFORMATION_MESSAGE);
            cards.show(holder, LOGIN);
            tag.setText(t("Smart Personal Finance","स्मार्ट पर्सनल फाइनेंस"));
            sub.setText(t("Sign in to continue","जारी रखने के लिए साइन इन करें"));
            if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(uLogin);
            uPass.requestFocusInWindow();
        }};
        sCreate.addActionListener(createAct);
        sName.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){sEmail.requestFocusInWindow();}});
        sEmail.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){sPhone.requestFocusInWindow();}});
        sPhone.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){sPass.requestFocusInWindow();}});
        sPass.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){sPass2.requestFocusInWindow();}});
        sPass2.addActionListener(createAct);

        // Reset action
        ActionListener resetAct=new ActionListener(){public void actionPerformed(ActionEvent e){
            String n=fName.getText().trim();
            if(n.length()==0){fErr.setText(t("Enter your user ID.","यूज़र ID दर्ज करें।"));return;}
            if(!users.contains(n)){fErr.setText(t("No such user.","ऐसा यूज़र नहीं है।"));return;}
            String em=userEmail.containsKey(n)?userEmail.get(n):"";
            if(em.length()==0){fErr.setText(t("No email is registered for this profile.","इस प्रोफ़ाइल के लिए कोई ईमेल रजिस्टर नहीं है।"));return;}
            if(!fEmail.getText().trim().equalsIgnoreCase(em)){fErr.setText(t("That email doesn't match our records.","यह ईमेल हमारे रिकॉर्ड से मेल नहीं खाता।"));return;}
            String np=new String(fPass.getPassword()).trim();
            if(np.length()==0){fErr.setText(t("Enter a new password.","नया पासवर्ड दर्ज करें।"));return;}
            userPassword.put(n,hashPassword(np));
            save();
            fErr.setText(" ");
            JOptionPane.showMessageDialog(frame,t("Password reset. You can log in now.","पासवर्ड रीसेट हो गया। अब आप लॉगिन कर सकते हैं।"),"ExpenseWise",JOptionPane.INFORMATION_MESSAGE);
            uName.setText(n); uPass.setText(""); uErr.setText(" ");
            cards.show(holder, LOGIN);
            tag.setText(t("Smart Personal Finance","स्मार्ट पर्सनल फाइनेंस"));
            sub.setText(t("Sign in to continue","जारी रखने के लिए साइन इन करें"));
            if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(uLogin);
            uPass.requestFocusInWindow();
        }};
        fReset.addActionListener(resetAct);
        fName.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){fEmail.requestFocusInWindow();}});
        fEmail.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){fPass.requestFocusInWindow();}});
        fPass.addActionListener(resetAct);

        GridBagConstraints rg=new GridBagConstraints(); rg.gridx=0;rg.gridy=0;rg.anchor=GridBagConstraints.CENTER;
        root.add(lcard,rg);
        frame.setContentPane(root);
        if(frame.getRootPane()!=null) frame.getRootPane().setDefaultButton(uLogin);
        frame.revalidate();frame.repaint();
        uName.requestFocusInWindow();
    }

    static void saveRememberedUser(String u){
        try{
            Files.createDirectories(DIR);
            Properties p=new Properties();
            if(Files.exists(PREF)){InputStream i=Files.newInputStream(PREF);p.load(i);i.close();}
            if(u.length()>0) p.setProperty("rememberUser",u);
            else p.remove("rememberUser");
            OutputStream o=Files.newOutputStream(PREF);
            p.store(o,"ExpenseWise preferences");
            o.close();
        }catch(IOException ignored){}
    }
    static String loadRememberedUser(){
        try{
            Properties p=new Properties();
            if(Files.exists(PREF)){InputStream i=Files.newInputStream(PREF);p.load(i);i.close();}
            return p.getProperty("rememberUser","");
        }catch(Exception e){return "";}
    }

    static void rebuild(){setPalette();applyUiManagerColors();frame.getContentPane().removeAll();build();frame.revalidate();frame.repaint();}
    static void exitApp(){
        int c=JOptionPane.showConfirmDialog(frame,t("Are you sure you want to exit ExpenseWise?","क्या आप वाकई ExpenseWise से बाहर निकलना चाहते हैं?"),t("Exit","बाहर निकलें"),JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
        if(c==JOptionPane.YES_OPTION){ save(); System.exit(0); }
    }

    static double sum(String type){double s=0;for(Tx x:filteredForCurrentUser())if(x.type.equals(type))s+=x.amount;return s;}
    static double monthExpense(){String m=YearMonth.now().toString();double s=0;for(Tx x:filteredForCurrentUser())if(x.type.equals("Expense")&&x.date.startsWith(m))s+=x.amount;return s;}
    static double monthIncome(){String m=YearMonth.now().toString();double s=0;for(Tx x:filteredForCurrentUser())if(x.type.equals("Income")&&x.date.startsWith(m))s+=x.amount;return s;}
    static String displayDate(String s){try{return LocalDate.parse(s).format(DISPLAY);}catch(Exception e){return s;}}
    static long nextId(){long m=0;for(Tx x:txs)m=Math.max(m,x.id);return m+1;}
    static Tx find(long id){for(Tx x:txs)if(x.id==id&&x.user.equals(user))return x;return null;}
    static List<Tx> filteredForCurrentUser(){List<Tx> l=new ArrayList<Tx>();for(Tx x:txs)if(x.user.equals(user))l.add(x);return l;}
    static String[] concat(String[] a,String[] b){String[] z=new String[a.length+b.length];System.arraycopy(a,0,z,0,a.length);System.arraycopy(b,0,z,a.length,b.length);return z;}
    static String[] months(){String[] a=new String[13];a[0]=t("All months","सभी महीने");YearMonth now=YearMonth.now();for(int i=1;i<13;i++)a[i]=now.minusMonths(i-1).toString();return a;}

    static void materializeRecurring(){
        YearMonth now=YearMonth.now();
        List<Tx> base=new ArrayList<Tx>(txs);
        Set<String> existing=new HashSet<String>();
        for(Tx x:txs){ if(x.recurring) existing.add(recurKey(x)); }
        long maxId=0; for(Tx x:txs) maxId=Math.max(maxId,x.id);
        List<Tx> toAdd=new ArrayList<Tx>();
        for(Tx b:base){
            if(!b.recurring) continue;
            try{
                LocalDate start=LocalDate.parse(b.date);
                YearMonth ym=YearMonth.from(start);
                while(ym.isBefore(now)){
                    ym=ym.plusMonths(1);
                    int day=Math.min(start.getDayOfMonth(),ym.lengthOfMonth());
                    LocalDate d=ym.atDay(day);
                    String key=b.user+"|"+b.type+"|"+b.category+"|"+b.description+"|"+b.amount+"|"+d.format(DF);
                    if(!existing.contains(key)){
                        existing.add(key);
                        maxId++;
                        toAdd.add(new Tx(maxId,b.user,b.type,d.format(DF),b.category,b.description,b.amount,true));
                    }
                }
            }catch(Exception ignored){}
        }
        txs.addAll(toAdd);
    }
    static String recurKey(Tx x){ return x.user+"|"+x.type+"|"+x.category+"|"+x.description+"|"+x.amount+"|"+x.date; }

    static void save(){try{Files.createDirectories(DIR);BufferedWriter w=Files.newBufferedWriter(DATA,StandardCharsets.UTF_8);w.write("id\tuser\ttype\tdate\tcategory\tdescription\tamount\trecurring\n");for(Tx x:txs)w.write(x.id+"\t"+esc(x.user)+"\t"+x.type+"\t"+x.date+"\t"+esc(x.category)+"\t"+esc(x.description)+"\t"+x.amount+"\t"+x.recurring+"\n");w.close();Files.write(USERS,users,StandardCharsets.UTF_8);saveUserDetails();savePrefs();}catch(IOException e){if(frame!=null)JOptionPane.showMessageDialog(frame,"Could not save data: "+e.getMessage());}}
    static void saveUserDetails(){try{Files.createDirectories(DIR);BufferedWriter w=Files.newBufferedWriter(USERDETAILS,StandardCharsets.UTF_8);for(String u:users)w.write(esc(u)+"\t"+esc(userEmail.containsKey(u)?userEmail.get(u):"")+"\t"+esc(userPhone.containsKey(u)?userPhone.get(u):"")+"\t"+esc(userPassword.containsKey(u)?userPassword.get(u):"")+"\t"+esc(userPhoto.containsKey(u)?userPhoto.get(u):"")+"\t0\n");w.close();}catch(IOException ignored){}}
    static void loadUserDetails(){try{if(Files.exists(USERDETAILS))for(String line:Files.readAllLines(USERDETAILS,StandardCharsets.UTF_8)){String[] p=line.split("\t",-1);if(p.length>=3){userEmail.put(p[0],p[1]);userPhone.put(p[0],p[2]);}if(p.length>=4){userPassword.put(p[0],p[3]);}if(p.length>=5&&p[4].length()>0){userPhoto.put(p[0],p[4]);}}}catch(IOException ignored){}}
    static String esc(String s){return s.replace("\t"," ").replace("\n"," ").replace("\r"," ");}
    static String hashPassword(String s){
        try{
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] d=md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder();
            for(byte b:d) sb.append(String.format("%02x",b));
            return sb.toString();
        }catch(Exception e){ return s; }
    }
    static boolean isHashed(String s){ return s!=null && s.length()==64 && s.matches("[0-9a-f]{64}"); }
    static boolean checkPassword(String stored,String entered){
        if(stored==null||stored.length()==0) return true;
        if(isHashed(stored)) return stored.equals(hashPassword(entered));
        return stored.equals(entered);
    }
    static void load(){try{Files.createDirectories(DIR);if(Files.exists(USERS))users.addAll(Files.readAllLines(USERS,StandardCharsets.UTF_8));if(users.isEmpty())users.add("Default");loadUserDetails();if(Files.exists(DATA)){for(String line:Files.readAllLines(DATA,StandardCharsets.UTF_8)){if(line.startsWith("id\t"))continue;String[] p=line.split("\t",-1);if(p.length>=8)try{txs.add(new Tx(Long.parseLong(p[0]),p[1],p[2],p[3],p[4],p[5],Double.parseDouble(p[6]),Boolean.parseBoolean(p[7])));}catch(Exception ignored){}}}if(Files.exists(PREF)){Properties pr=new Properties();InputStream in=Files.newInputStream(PREF);pr.load(in);in.close();user=pr.getProperty("user",users.get(0));lang=pr.getProperty("lang","English");currency=pr.getProperty("currency","₹");try{theme=Theme.valueOf(pr.getProperty("theme","LIGHT"));}catch(Exception ignored){}if(!users.contains(user))user=users.get(0);}}catch(IOException ignored){}}
    static void savePrefs(){try{Files.createDirectories(DIR);Properties p=new Properties();if(Files.exists(PREF)){InputStream i=Files.newInputStream(PREF);p.load(i);i.close();}p.setProperty("user",user);p.setProperty("lang",lang);p.setProperty("theme",theme.name());p.setProperty("currency",currency);OutputStream o=Files.newOutputStream(PREF);p.store(o,"ExpenseWise preferences");o.close();}catch(IOException ignored){}}
    static double loadBudget(){try{Properties p=new Properties();if(Files.exists(BUDGET)){InputStream i=Files.newInputStream(BUDGET);p.load(i);i.close();}return Double.parseDouble(p.getProperty(user,"0"));}catch(Exception e){return 0;}}
    static void saveBudget(double v){try{Properties p=new Properties();if(Files.exists(BUDGET)){InputStream i=Files.newInputStream(BUDGET);p.load(i);i.close();}p.setProperty(user,String.valueOf(v));OutputStream o=Files.newOutputStream(BUDGET);p.store(o,"ExpenseWise budgets");o.close();}catch(IOException ignored){}}
    static void showAddBudgetDialog(){
        final JDialog dlg=new JDialog(frame,t("Add Budget","बजट जोड़ें"),true); dlg.setResizable(false);
        JPanel p=new JPanel();p.setBackground(CARD);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS)); p.setBorder(new EmptyBorder(22,24,22,24));
        JLabel title=text(t("Set monthly budget","मासिक बजट सेट करें"),16,Font.BOLD,TEXT);title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub=text(t("Enter how much you plan to spend this month.","इस महीने आप कितना खर्च करने की योजना बना रहे हैं।"),11,Font.PLAIN,MUTED);sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(title);p.add(Box.createVerticalStrut(4));p.add(sub);p.add(Box.createVerticalStrut(16));
        JLabel lbl=text(t("Budget amount","बजट राशि")+" ("+currency+")",11,Font.BOLD,MUTED);lbl.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(lbl);p.add(Box.createVerticalStrut(6));
        double existing=loadBudget();
        final JTextField amt=new JTextField(existing>0?String.valueOf(existing):"");
        styleInput(amt);amt.setAlignmentX(Component.LEFT_ALIGNMENT);amt.setMaximumSize(new Dimension(2000,40));amt.setPreferredSize(new Dimension(320,40));
        p.add(amt);p.add(Box.createVerticalStrut(18));
        final JLabel err=text("",11,Font.PLAIN,RED);err.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(err);p.add(Box.createVerticalStrut(6));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setOpaque(false);actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton cancel=button(t("Cancel","रद्द करें"),BG,TEXT);cancel.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){dlg.dispose();}});
        JButton save=button(t("Save budget","बजट सेव करें"),ACCENT,Color.WHITE);
        ActionListener saveAct=new ActionListener(){public void actionPerformed(ActionEvent e){ try{ double v=Double.parseDouble(amt.getText().trim()); if(v<0)throw new NumberFormatException(); saveBudget(v); dlg.dispose(); JOptionPane.showMessageDialog(frame,t("Budget saved.","बजट सेव हो गया।")); showSettings(); }catch(Exception ex){ err.setText(t("Enter a valid amount.","सही राशि दर्ज करें।")); } }};
        save.addActionListener(saveAct);
        amt.addActionListener(saveAct);
        actions.add(cancel);actions.add(save);p.add(actions);
        dlg.getRootPane().setDefaultButton(save);
        dlg.setContentPane(p);dlg.pack();dlg.setLocationRelativeTo(frame);dlg.setVisible(true);
    }

    static void exportExcel(List<Tx> list){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("ExpenseWise-"+user+".xlsx"));if(fc.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;try{Xlsx.write(fc.getSelectedFile().toPath(),list);JOptionPane.showMessageDialog(frame,t("Excel exported successfully.","Excel सफलतापूर्वक export हो गया।"));}catch(Exception e){JOptionPane.showMessageDialog(frame,"Export failed: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}}
    static void exportPdf(List<Tx> list){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("ExpenseWise-"+user+".pdf"));if(fc.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;try{Pdf.write(fc.getSelectedFile().toPath(),list);JOptionPane.showMessageDialog(frame,t("PDF exported successfully.","PDF सफलतापूर्वक export हो गया।"));}catch(Exception e){JOptionPane.showMessageDialog(frame,"Export failed: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}}
    static void exportCsv(List<Tx> list){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new File("ExpenseWise-"+user+".csv"));if(fc.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;try{Csv.write(fc.getSelectedFile().toPath(),list);JOptionPane.showMessageDialog(frame,t("CSV exported successfully.","CSV सफलतापूर्वक export हो गया।"));}catch(Exception e){JOptionPane.showMessageDialog(frame,"Export failed: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}}
    static class Csv{
        static void write(Path path,List<Tx> l)throws IOException{
            BufferedWriter w=Files.newBufferedWriter(path,StandardCharsets.UTF_8);
            w.write(field("Date")+","+field("Description")+","+field("Category")+","+field("Type")+","+field("Amount")+","+field("Recurring")+"\n");
            for(Tx x:l){
                w.write(field(x.date)+","+field(x.description)+","+field(x.category)+","+field(x.type)+","+field(String.format(Locale.US,"%.2f",x.amount))+","+field(String.valueOf(x.recurring))+"\n");
            }
            w.close();
        }
        static String field(String s){
            if(s==null)s="";
            boolean needsQuotes=s.contains(",")||s.contains("\"")||s.contains("\n")||s.contains("\r");
            String v=s.replace("\"","\"\"");
            return needsQuotes?"\""+v+"\"":v;
        }
    }
    static class Xlsx{static void write(Path path,List<Tx> l)throws Exception{ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(path));put(z,"[Content_Types].xml","<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");put(z,"_rels/.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");put(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");put(z,"xl/workbook.xml","<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Transactions\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");StringBuilder s=new StringBuilder("<?xml version=\"1.0\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");s.append(row(new String[]{"Date","Description","Category","Type","Amount","Recurring"}));for(Tx x:l)s.append(row(new String[]{x.date,x.description,x.category,x.type,String.format(Locale.US,"%.2f",x.amount),String.valueOf(x.recurring)}));s.append("</sheetData></worksheet>");put(z,"xl/worksheets/sheet1.xml",s.toString());z.close();}static String row(String[] a){StringBuilder s=new StringBuilder("<row>");for(String v:a)s.append("<c t=\"inlineStr\"><is><t>").append(xml(v)).append("</t></is></c>");return s.append("</row>").toString();}static String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}static void put(ZipOutputStream z,String n,String s)throws IOException{z.putNextEntry(new ZipEntry(n));z.write(s.getBytes(StandardCharsets.UTF_8));z.closeEntry();}}
    static class Pdf{
        static final String[] HEAD={"Date","Description","Category","Type","Amount"};
        static final double[] COLW={65,175,90,70,90};
        static final double MARGIN_X=40,PAGE_W=595,PAGE_H=842,TOP_Y=800,BOTTOM_Y=60,ROW_H=20,HEAD_FS=10,ROW_FS=9,TITLE_FS=14;
        static double tableWidth(){double s=0;for(double w:COLW)s+=w;return s;}
        static double charWidth(char c){ if(c==' ')return 0.28; if("iIl.,:;'|!".indexOf(c)>=0)return 0.30; if("mMW".indexOf(c)>=0)return 0.90; if(Character.isUpperCase(c))return 0.70; if(Character.isDigit(c))return 0.556; return 0.52; }
        static double textWidth(String s,double fs){double w=0;for(int i=0;i<s.length();i++)w+=charWidth(s.charAt(i));return w*fs;}
        static String fit(String s,double maxWidth,double fs){ if(textWidth(s,fs)<=maxWidth)return s; String ell="...";StringBuilder b=new StringBuilder(); for(int i=0;i<s.length();i++){String cand=b.toString()+s.charAt(i);if(textWidth(cand,fs)+textWidth(ell,fs)>maxWidth)break;b.append(s.charAt(i));} return b.toString()+ell; }
        static String escPdf(String s){return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)");}
        static String fmt(double d){return d==Math.rint(d)?String.valueOf((long)d):String.format(Locale.US,"%.2f",d);}
        static String textOp(String s,double x,double y,double fs,boolean bold){return "BT /"+(bold?"F2":"F1")+" "+fmt(fs)+" Tf "+fmt(x)+" "+fmt(y)+" Td ("+escPdf(s)+") Tj ET\n";}
        static String lineOp(double x1,double y1,double x2,double y2){return fmt(x1)+" "+fmt(y1)+" m "+fmt(x2)+" "+fmt(y2)+" l S\n";}
        static void centered(StringBuilder cur,String s,double colX,double colW,double y,double fs,boolean bold){ String v=fit(s,colW-8,fs);double tw=textWidth(v,fs);cur.append(textOp(v,colX+(colW-tw)/2.0,y,fs,bold)); }
        static double drawHeader(StringBuilder cur,double y){ double cx=MARGIN_X;for(int i=0;i<HEAD.length;i++){centered(cur,HEAD[i],cx,COLW[i],y,HEAD_FS,true);cx+=COLW[i];} double lineY=y-6;cur.append(lineOp(MARGIN_X,lineY,MARGIN_X+tableWidth(),lineY));return lineY-16; }
        static void write(Path path,List<Tx> l)throws Exception{
            List<String> pageStreams=new ArrayList<String>();
            boolean firstPage=true;int i=0;
            do{
                StringBuilder cur=new StringBuilder();double y=TOP_Y;
                if(firstPage){cur.append(textOp("ExpenseWise - "+user,MARGIN_X,y,TITLE_FS,true));y-=20;cur.append(textOp("Generated: "+LocalDate.now(),MARGIN_X,y,9,false));y-=26;firstPage=false;}
                double headTop=y+HEAD_FS+6;
                y=drawHeader(cur,y);
                double firstRowY=y;int rows=0;
                while(i<l.size()&&y>=BOTTOM_Y){ Tx x=l.get(i); String[] vals={x.date,x.description==null||x.description.length()==0?"-":x.description,x.category,x.type,String.format(Locale.US,"%.2f",x.amount)}; double cx=MARGIN_X;for(int c=0;c<vals.length;c++){centered(cur,vals[c],cx,COLW[c],y,ROW_FS,false);cx+=COLW[c];} y-=ROW_H;i++;rows++; }
                int effRows=Math.max(rows,1); double bodyBottom=firstRowY-effRows*ROW_H+ROW_H-4;
                cur.append(lineOp(MARGIN_X,bodyBottom,MARGIN_X+tableWidth(),bodyBottom));
                double cxLine=MARGIN_X;for(int c=0;c<=COLW.length;c++){cur.append(lineOp(cxLine,headTop,cxLine,bodyBottom));if(c<COLW.length)cxLine+=COLW[c];}
                if(l.isEmpty())cur.append(textOp("No transactions to show.",MARGIN_X,firstRowY,ROW_FS,false));
                pageStreams.add(cur.toString());
            }while(i<l.size());
            int n=pageStreams.size(); List<String> objs=new ArrayList<String>(); StringBuilder kids=new StringBuilder();
            for(int k=0;k<n;k++){if(k>0)kids.append(" ");kids.append(3+2*k).append(" 0 R");}
            int fontRegNum=3+2*n, fontBoldNum=fontRegNum+1;
            objs.add("<< /Type /Catalog /Pages 2 0 R >>");
            objs.add("<< /Type /Pages /Kids ["+kids+"] /Count "+n+" >>");
            for(int k=0;k<n;k++){ int pageNum=3+2*k, contentNum=pageNum+1; objs.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "+(int)PAGE_W+" "+(int)PAGE_H+"] /Resources << /Font << /F1 "+fontRegNum+" 0 R /F2 "+fontBoldNum+" 0 R >> >> /Contents "+contentNum+" 0 R >>"); byte[] st=pageStreams.get(k).getBytes(StandardCharsets.US_ASCII); objs.add("<< /Length "+st.length+" >>\nstream\n"+pageStreams.get(k)+"\nendstream"); }
            objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
            objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
            ByteArrayOutputStream out=new ByteArrayOutputStream();wa(out,"%PDF-1.4\n"); List<Integer> offs=new ArrayList<Integer>();
            for(int idx=0;idx<objs.size();idx++){offs.add(out.size());wa(out,(idx+1)+" 0 obj\n");wa(out,objs.get(idx));wa(out,"\nendobj\n");}
            int xref=out.size(); wa(out,"xref\n0 "+(objs.size()+1)+"\n0000000000 65535 f \n");
            for(int o:offs)wa(out,String.format("%010d 00000 n \n",o));
            wa(out,"trailer << /Size "+(objs.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF");
            Files.write(path,out.toByteArray());
        }
        static void wa(OutputStream o,String s)throws IOException{o.write(s.getBytes(StandardCharsets.US_ASCII));}
    }
}