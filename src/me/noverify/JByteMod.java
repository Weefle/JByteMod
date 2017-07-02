package me.noverify;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.util.JarUtils;
import me.noverify.list.CellRenderer;
import me.noverify.list.InsnListEntry;
import me.noverify.list.ListEntry;
import me.noverify.list.SearchListEntry;
import me.noverify.utils.DisplayUtils;
import me.noverify.utils.EditDialogue;
import me.noverify.utils.PopupMenu;
import me.noverify.utils.SortedTreeNode;

public class JByteMod extends JFrame {

	private JPanel contentPane;
	private JTree fileTree;
	private JTabbedPane rightSide;
	private JPanel leftSide;
	private JList<ListEntry> codeList;
	private JList<SearchListEntry> searchList;
	public static JByteMod instance;
	private JMenuBar menuBar;
	private JMenu mnFile;
	private JMenu mnTools;
	private JMenuItem mntmSave;
	private JMenuItem mntmSaveAs;
	private JMenuItem mntmLoad;
	private JMenuItem mntmSearch;
	private JMenuItem mntmClose;
	private Map<String, ClassNode> classes;
	private Map<String, byte[]> output;
	private JLabel rightDesc;
	private JLabel searchDesc;
	private File opened;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
						if ("Nimbus".equals(info.getName())) {
							UIManager.setLookAndFeel(info.getClassName());
							break;
						}
					}
					instance = new JByteMod();
					instance.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	/**
	 * Create the frame.
	 */
	public JByteMod() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				if (JOptionPane.showConfirmDialog(instance, "Do you really want to exit? All unsaved changes will be lost.", "Are you sure?",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					System.exit(0);
				}
			}
		});
		setBounds(100, 100, 1280, 720);
		setTitle("JByteMod");

		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		mnFile = new JMenu("File");
		menuBar.add(mnFile);

		mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (opened != null) {
					saveJarFile(opened);
				}
			}
		});
		mntmSave.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mnFile.add(mntmSave);

		mntmSaveAs = new JMenuItem("Save As..");
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (opened != null) {
					saveAsFileChooser();
				}
			}
		});
		mnFile.add(mntmSaveAs);

		mntmLoad = new JMenuItem("Load");
		mntmLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openFileChooserLoad();
			}
		});
		mntmLoad.setAccelerator(KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mnFile.add(mntmLoad);

		mntmClose = new JMenuItem("Close");
		mntmClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (opened == null || JOptionPane.showConfirmDialog(instance, "Do you really want to exit? All unsaved changes will be lost.",
						"Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					System.exit(0);
				}
			}
		});
		mntmClose.setAccelerator(KeyStroke.getKeyStroke('W', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mnFile.add(mntmClose);

		mnTools = new JMenu("Tools");
		menuBar.add(mnTools);

		mntmSearch = new JMenuItem("Search");
		mntmSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (classes == null) {
					EditDialogue.error("Open a jar file first.");
					return;
				}
				final JPanel panel = new JPanel(new BorderLayout(5, 5));
				final JPanel input = new JPanel(new GridLayout(0, 1));
				final JPanel labels = new JPanel(new GridLayout(0, 1));
				panel.add(labels, "West");
				panel.add(input, "Center");
				panel.add(new JLabel("Warning: This could take some time\n on short strings!"), "South");
				labels.add(new JLabel("String Constant:"));
				JTextField cst = new JTextField();
				input.add(cst);
				JCheckBox exact = new JCheckBox("Exact");
				JCheckBox snstv = new JCheckBox("Case sensitive");
				labels.add(exact);
				input.add(snstv);
				if (JOptionPane.showConfirmDialog(JByteMod.instance, panel, "Search LDC", 2) == JOptionPane.OK_OPTION
						&& !cst.getText().isEmpty()) {
					searchForCst(cst.getText(), exact.isSelected(), snstv.isSelected());
				}
			}
		});
		mntmSearch.setAccelerator(KeyStroke.getKeyStroke('H', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mnTools.add(mntmSearch);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		SortedTreeNode root = new SortedTreeNode("", null, null);
		DefaultTreeModel treeModel = new DefaultTreeModel(root);
		fileTree = new JTree(treeModel);
		fileTree.setRootVisible(false);
		fileTree.setShowsRootHandles(true);
		fileTree.setCellRenderer(new CellRenderer());
		fileTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				SortedTreeNode node = (SortedTreeNode) fileTree.getLastSelectedPathComponent();
				if (node == null)
					return;
				System.out.println(node.getCn());
				if (node.getCn() != null && node.getMn() != null) {
					decompileMethod(node.getCn(), node.getMn());
				} else if (node.getCn() != null) {
					decompileClass(node.getCn());
				} else {
					fileTree.clearSelection();
					if (node.isLeaf()) {
						return;
					}
					if (fileTree.isExpanded(e.getPath())) {
						fileTree.collapsePath(e.getPath());
					} else {
						fileTree.expandPath(e.getPath());
					}
				}
			}
		});
		rightSide = new JTabbedPane();
		leftSide = new JPanel();
		leftSide.setLayout(new BorderLayout(0, 0));
		//		leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));
		leftSide.add(new JLabel(" Jar File"), BorderLayout.NORTH);
		leftSide.add(new JScrollPane(fileTree), BorderLayout.CENTER);
		setupTabs();
		JPanel border = new JPanel();
		border.setBorder(new LineBorder(Color.GRAY));
		border.setLayout(new GridLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSide, rightSide);
		splitPane.setDividerLocation(150);
		splitPane.setContinuousLayout(true);
		JPanel b2 = new JPanel();
		b2.setBorder(new EmptyBorder(5, 0, 5, 0));
		b2.setLayout(new GridLayout());
		b2.add(splitPane);
		border.add(b2);
		contentPane.add(border, BorderLayout.CENTER);
	}

	protected void searchForCst(String search, boolean exact, boolean caseSens) {
		searchDesc.setText("Results for \"" + search + "\"");
		rightSide.setSelectedIndex(1);
		DefaultListModel<SearchListEntry> lm = (DefaultListModel<SearchListEntry>) searchList.getModel();
		lm.clear();
		if (!caseSens) {
			search = search.toLowerCase();
		}
		for (ClassNode c : classes.values()) {
			for (MethodNode m : c.methods) {
				for (AbstractInsnNode ain : m.instructions.toArray()) {
					if (ain instanceof LdcInsnNode) {
						String cst = ((LdcInsnNode) ain).cst.toString();
						if (!caseSens) {
							cst = cst.toLowerCase();
						}
						if (!exact && cst.toString().contains(search) || cst.toString().equals(search)) {
							lm.addElement(new SearchListEntry(c, m, cst));
						}
					}
				}
			}
		}
	}

	protected void decompileClass(ClassNode cn) {
		rightSide.setSelectedIndex(0);
		DefaultListModel<ListEntry> lm = (DefaultListModel<ListEntry>) codeList.getModel();
		lm.clear();
	}

	protected void decompileMethod(ClassNode cn, MethodNode mn) {
		rightSide.setSelectedIndex(0);
		DefaultListModel<ListEntry> lm = (DefaultListModel<ListEntry>) codeList.getModel();
		lm.clear();
		rightDesc.setText(cn.name + "." + mn.name + mn.desc);
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			lm.addElement(new InsnListEntry(mn, ain));
		}
	}

	protected void openFileChooserLoad() {
		JFileChooser jfc = new JFileChooser(new File(System.getProperty("user.home") + "/Desktop"));
		//		jfc.addChoosableFileFilter(new FileNameExtensionFilter("Java Archives", "jar", "zip"));
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.setFileFilter(new FileNameExtensionFilter("Java Archives", "jar", "zip"));
		int result = jfc.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File input = jfc.getSelectedFile();
			this.opened = input;
			System.out.println("Selected input jar: " + input.getAbsolutePath());
			loadJarFile(input);
		}
	}

	protected void saveAsFileChooser() {
		JFileChooser jfc = new JFileChooser(new File(System.getProperty("user.home") + "/Desktop"));
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.setFileFilter(new FileNameExtensionFilter("Java Archives", "jar", "zip"));
		int result = jfc.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File output = jfc.getSelectedFile();
			System.out.println("Selected output jar: " + output.getAbsolutePath());
			saveJarFile(output);
		}
	}

	private void saveJarFile(File op) {
		System.out.println("Writing..");
		for (String s : classes.keySet()) {
			ClassNode node = classes.get(s);
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			node.accept(writer);
			this.output.put(s, writer.toByteArray());
		}
		System.out.println("Saving..");
		JarUtils.saveAsJar(output, op.getAbsolutePath());
		System.out.println("Done!");
	}

	private void loadJarFile(File input) {
		try {
			classes = JarUtils.loadClasses(input);
			output = JarUtils.loadNonClassEntries(input);
			setupTree();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void setupTree() {
		DefaultTreeModel tm = (DefaultTreeModel) fileTree.getModel();
		SortedTreeNode root = (SortedTreeNode) tm.getRoot();
		root.removeAllChildren();
		tm.reload();

		for (ClassNode c : classes.values()) {
			for (MethodNode m : c.methods) {
				String name = c.name + ".class/" + m.name;
				if (name.isEmpty())
					continue;
				if (!name.contains("/")) {
					root.add(new SortedTreeNode(name, c, m));
				} else {
					String[] names = name.split("/");
					SortedTreeNode node = root;
					int i = 1;
					for (String n : names) {
						SortedTreeNode newnode = new SortedTreeNode(n, i >= names.length - 1 ? c : null, null);
						if (i == names.length) {
							newnode.setMn(m);
							node.add(newnode);
							//						tm.insertNodeInto(newnode, node, node.getChildCount());
							tm.getChildCount(node);
						} else {
							SortedTreeNode extnode = addUniqueNode(tm, node, newnode);
							if (extnode != null) {
								node = extnode;
							} else {
								node = newnode;
							}
						}
						i++;
					}
				}
			}
		}
		sort(tm, root);
		tm.reload();
		fileTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent me) {
				if (SwingUtilities.isRightMouseButton(me)) {
					TreePath tp = fileTree.getPathForLocation(me.getX(), me.getY());
					if (tp != null && tp.getParentPath() != null) {
						fileTree.setSelectionPath(tp);
						MethodNode mn = ((SortedTreeNode) fileTree.getLastSelectedPathComponent()).getMn();
						ClassNode cn = ((SortedTreeNode) fileTree.getLastSelectedPathComponent()).getCn();
						if (mn != null) {
							JPopupMenu menu = new JPopupMenu();
							JMenuItem edit = new JMenuItem("Edit");
							edit.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									EditDialogue.createMethodDialogue(mn);
								}
							});
							menu.add(edit);
							menu.show(fileTree, me.getX(), me.getY());
						} else if (cn != null) {
							JPopupMenu menu = new JPopupMenu();
							JMenuItem edit = new JMenuItem("Edit");
							edit.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									EditDialogue.createClassDialogue(cn);
								}
							});
							menu.add(edit);
							menu.show(fileTree, me.getX(), me.getY());
						}
					}
				}
			}
		});
	}

	public SortedTreeNode addUniqueNode(DefaultTreeModel model, SortedTreeNode node, SortedTreeNode childNode) {
		for (int i = 0; i < model.getChildCount(node); i++) {
			Object compUserObj = ((SortedTreeNode) model.getChild(node, i)).getUserObject();
			if (compUserObj.equals(childNode.getUserObject())) {
				return (SortedTreeNode) model.getChild(node, i);
			}
		}
		node.add(childNode);
		return null;
	}

	public void sort(DefaultTreeModel model, SortedTreeNode node) {
		if (!node.isLeaf()) {
			node.sort();
			for (int i = 0; i < model.getChildCount(node); i++) {
				SortedTreeNode child = ((SortedTreeNode) model.getChild(node, i));
				sort(model, child);
			}
		}
	}

	private void setupTabs() {
		codeList = new JList<ListEntry>(new DefaultListModel());
		codeList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		codeList.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					PopupMenu.showPopupInsn(e, codeList);
				}
			}
		});
		searchList = new JList<SearchListEntry>(new DefaultListModel());
		searchList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		searchList.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem decl = new JMenuItem("Go to declaration");
					decl.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ClassNode cn = searchList.getSelectedValue().getCn();
							MethodNode mn = searchList.getSelectedValue().getMn();
							decompileMethod(cn, mn);
						}
					});
					menu.add(decl);
					menu.show(searchList, e.getX(), e.getY());
				}
			}
		});
		JPanel code = new JPanel();
		code.setLayout(new BorderLayout(0, 0));
		JPanel lpad = new JPanel();
		lpad.setBorder(new EmptyBorder(1, 5, 0, 5));
		lpad.setLayout(new GridLayout());
		rightDesc = new JLabel(" ");
		lpad.add(rightDesc);
		code.add(lpad, BorderLayout.NORTH);
		code.add(new JScrollPane(codeList), BorderLayout.CENTER);
		rightSide.addTab("Code", code);

		JPanel search = new JPanel();
		search.setLayout(new BorderLayout(0, 0));
		JPanel lpad2 = new JPanel();
		lpad2.setBorder(new EmptyBorder(1, 5, 0, 5));
		lpad2.setLayout(new GridLayout());
		searchDesc = new JLabel(" ");
		lpad2.add(searchDesc);
		search.add(lpad2, BorderLayout.NORTH);
		search.add(new JScrollPane(searchList), BorderLayout.CENTER);
		rightSide.addTab("Search", search);
	}

	public void reloadList(MethodNode mn) {
		DefaultListModel<ListEntry> lm = (DefaultListModel<ListEntry>) codeList.getModel();
		lm.clear();
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			lm.addElement(new InsnListEntry(mn, ain));
		}
	}

	public void updateFileTree() {
		SortedTreeNode s = (SortedTreeNode) fileTree.getSelectionPath().getLastPathComponent();
		ClassNode cn = s.getCn();
		MethodNode mn = s.getMn();
		if (mn != null) {
			s.setUserObject(mn.name);
			((DefaultTreeModel) fileTree.getModel()).nodeChanged(s);
			decompileMethod(cn, mn);
		} else {
			String cname = cn.name;
			if (cn.name.contains("/")) {
				String[] spl = cn.name.split("/");
				cname = spl[spl.length - 1];
			}
			s.setUserObject(cname + ".class");
			((DefaultTreeModel) fileTree.getModel()).nodeChanged(s);
			for (String key : classes.keySet()) {//update mah shit
				if (classes.get(key).equals(cn)) {
					classes.remove(key);
					break;
				}
			}
			classes.put(cn.name, cn);
			decompileClass(cn);
		}
	}

}