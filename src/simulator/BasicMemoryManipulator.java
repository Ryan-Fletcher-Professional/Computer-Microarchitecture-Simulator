package simulator;

import javax.swing.*;
import java.awt.*;

import static main.GLOBALS.*;
import memory.MemoryModule;
import memory.MemoryRequest;

public class BasicMemoryManipulator extends JFrame
{
    private final int id;
    private int width, height;

    private JTextField addressField, valueField, columnSizeField, lineSizeField, returnField;
    private JTextArea displayText;
    private JRadioButton cacheRadio, ramRadio, dataRadio, instructionRadio, shortWordsRadio, longWordsRadio,
                         wordRadio, lineRadio, addressBinRadio, addressIntRadio, valueBinRadio, valueIntRadio;
    private JList<MemoryModule> instructionCachesList, dataCachesList, unifiedMemoryList;
    private DefaultListModel<MemoryModule> instructionCachesModel, dataCachesModel, unifiedMemoryModel;
    private MemoryModule currentlySelected;

    public BasicMemoryManipulator(int id)
    {
        this.id = id;
        new BasicMemoryManipulator(id, DEFAULT_UI_WIDTH, DEFAULT_UI_HEIGHT);
    }

    public BasicMemoryManipulator(int id, int width, int height)
    {
        this.id = id;
        this.width = width;
        this.height = height;

        setTitle("Basic Memory Manipulator");
        setSize(width, height);
        setLayout(new BorderLayout());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);

        // Toolbar at the top
        JToolBar toolBar = new JToolBar();
        JButton dummyButton1 = new JButton("TBI");
        JButton dummyButton2 = new JButton("TBI");
        toolBar.add(dummyButton1);
        toolBar.add(dummyButton2);
        add(toolBar, BorderLayout.NORTH);

        // Top left for control, top right for cache configuration view, bottom for state view
        JSplitPane topBottomPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        topBottomPane.setDividerLocation(height / 2);
        JSplitPane leftRightPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftRightPane.setDividerLocation(width / 2);

        // Left
        JPanel leftPanel = new JPanel(new GridLayout(2, 1));

        // Memory manipulation
        JPanel memoryOperationsPanel = new JPanel(new GridLayout(0, 1));  // 0 rows means "as many as needed"

        JPanel entryPanel = new JPanel( new GridLayout(3, 2));
        addressField = new JTextField(1);
        valueField = new JTextField(1);
        JPanel addressRadioPanel = new JPanel(new GridLayout(1, 2));
        addressBinRadio = new JRadioButton("Binary");
        addressIntRadio = new JRadioButton("Integer");
        ButtonGroup addressGroup = new ButtonGroup();
        addressGroup.add(addressBinRadio);
        addressGroup.add(addressIntRadio);
        addressBinRadio.setSelected(true);
        addressRadioPanel.add(addressBinRadio);
        addressRadioPanel.add(addressIntRadio);
        JPanel valueRadioPanel = new JPanel(new GridLayout(1, 2));
        valueBinRadio = new JRadioButton("Binary");
        valueIntRadio = new JRadioButton("Integer");
        ButtonGroup valueGroup = new ButtonGroup();
        valueGroup.add(valueBinRadio);
        valueGroup.add(valueIntRadio);
        valueBinRadio.setSelected(true);
        valueRadioPanel.add(valueBinRadio);
        valueRadioPanel.add(valueIntRadio);
        entryPanel.add(new JLabel("Address"));
        entryPanel.add(new JLabel("Value"));
        entryPanel.add(addressField);
        entryPanel.add(valueField);
        entryPanel.add(addressRadioPanel);
        entryPanel.add(valueRadioPanel);

        JPanel wordPanel = new JPanel(new GridLayout(1, 2));
        wordRadio = new JRadioButton("Word");
        lineRadio = new JRadioButton("Whole Line");
        ButtonGroup wordSizeLoadSelection = new ButtonGroup();
        wordSizeLoadSelection.add(wordRadio);
        wordSizeLoadSelection.add(lineRadio);
        wordRadio.setSelected(true);
        wordPanel.add(wordRadio);
        wordPanel.add(lineRadio);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton storeButton = new JButton("Store");
        JButton loadButton = new JButton("Load");
        storeButton.addActionListener(e -> storeWordInCurrentMemoryModule());
        loadButton.addActionListener(e -> loadCurrentMemoryModule());
        buttonPanel.add(storeButton);
        buttonPanel.add(loadButton);

        memoryOperationsPanel.add(entryPanel);
        memoryOperationsPanel.add(wordPanel);
        memoryOperationsPanel.add(buttonPanel);

        // MemoryModule creation
        JPanel memoryCreationPanel = new JPanel(new GridLayout(0, 2));

        cacheRadio = new JRadioButton("Cache");
        ramRadio = new JRadioButton("RAM");
        ButtonGroup memoryKindSelection = new ButtonGroup();
        memoryKindSelection.add(cacheRadio);
        memoryKindSelection.add(ramRadio);
        cacheRadio.setSelected(true);

        dataRadio = new JRadioButton("Data Memory");
        instructionRadio = new JRadioButton("Instruction Memory");
        ButtonGroup memoryTypeSelection = new ButtonGroup();
        memoryTypeSelection.add(dataRadio);
        memoryTypeSelection.add(instructionRadio);
        dataRadio.setSelected(true);

        JPanel columnSizePanel = new JPanel(new GridLayout(2, 1));
        columnSizePanel.add(new JLabel("Column Size"));
        columnSizeField = new JTextField(32);
        columnSizePanel.add(columnSizeField);
        JPanel lineSizePanel = new JPanel(new GridLayout(2, 1));
        lineSizePanel.add(new JLabel("Line Size"));
        lineSizeField = new JTextField(32);
        lineSizePanel.add(lineSizeField);

        shortWordsRadio = new JRadioButton("32-Bit Words");
        longWordsRadio = new JRadioButton("64-Bit Words");
        ButtonGroup wordSizeSelection = new ButtonGroup();
        wordSizeSelection.add(shortWordsRadio);
        wordSizeSelection.add(longWordsRadio);
        shortWordsRadio.setSelected(true);

        memoryCreationPanel.add(cacheRadio);
        memoryCreationPanel.add(ramRadio);
        memoryCreationPanel.add(dataRadio);
        memoryCreationPanel.add(instructionRadio);
        memoryCreationPanel.add(columnSizePanel);
        memoryCreationPanel.add(lineSizePanel);
        memoryCreationPanel.add(shortWordsRadio);
        memoryCreationPanel.add(longWordsRadio);

        leftPanel.add(memoryOperationsPanel);
        leftPanel.add(memoryCreationPanel);

        // Right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        unifiedMemoryModel = new DefaultListModel<>();
        instructionCachesModel = new DefaultListModel<>();
        dataCachesModel = new DefaultListModel<>();
        unifiedMemoryList = new JList<>(unifiedMemoryModel);
        instructionCachesList = new JList<>(instructionCachesModel);
        dataCachesList = new JList<>(dataCachesModel);

        rightPanel.add(createListPanel("Unified Memory", unifiedMemoryList, new JList[] {instructionCachesList, dataCachesList}));
        JPanel cachesPanel = new JPanel(new GridLayout(1, 2));
        cachesPanel.add(createListPanel("Instruction Caches", instructionCachesList, new JList[] {dataCachesList, unifiedMemoryList}));
        cachesPanel.add(createListPanel("Data Caches", dataCachesList, new JList[] {instructionCachesList, unifiedMemoryList}));
        rightPanel.add(cachesPanel);

        // Bottom
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        returnField = new JTextField();
        returnField.setHorizontalAlignment(JTextField.CENTER);
        returnField.setPreferredSize(new Dimension(500, 30));
        returnField.setEditable(false);
        displayText = new JTextArea();
        displayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        displayText.setEditable(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        bottomPanel.add(returnField, gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        JScrollPane displayPane = new JScrollPane(displayText);
        bottomPanel.add(displayPane, gbc);

        // Finish arranging window
        leftRightPane.setLeftComponent(leftPanel);
        leftRightPane.setRightComponent(new JScrollPane(rightPanel));

        topBottomPane.setTopComponent(leftRightPane);
        topBottomPane.setBottomComponent(bottomPanel);

        this.add(topBottomPane, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private JPanel createListPanel(String title, JList<MemoryModule> list, JList[] otherLists) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JButton addButton = new JButton("+");
        addButton.addActionListener(e -> createNewMemoryModule(list));
        panel.add(addButton, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting() && (list.getSelectedValue() != null))
            {
                currentlySelected = list.getSelectedValue();
                for(JList other : otherLists)
                {
                    other.clearSelection();
                }
                displayText.setText(currentlySelected.getMemoryDisplay());
            }
        });

        return panel;
    }

    private void createNewMemoryModule(JList<MemoryModule> list)
    {
        DefaultListModel<MemoryModule> model = (DefaultListModel<MemoryModule>)list.getModel();
        MemoryModule newModule;
        try
        {
            newModule = new MemoryModule(GET_ID(),
                                         cacheRadio.isSelected() ? MEMORY_KIND.CACHE : MEMORY_KIND.RAM,
                                         dataRadio.isSelected() ? MEMORY_TYPE.DATA : MEMORY_TYPE.INSTRUCTION,
                                         shortWordsRadio.isSelected() ? WORD_LENGTH.SHORT : WORD_LENGTH.LONG,
                                         WRITE_MODE.THROUGH_ALLOCATE,
                                         model.getSize() > 0 ? model.getElementAt(model.getSize() - 1) :
                                                 (unifiedMemoryModel.getSize() > 0 ?
                                                  unifiedMemoryModel.getElementAt(unifiedMemoryModel.getSize() - 1) :
                                                  null),
                                         Integer.parseInt(columnSizeField.getText()),
                                         Integer.parseInt(lineSizeField.getText()));
            model.addElement(newModule);
        }
        catch(NumberFormatException e)
        {
            System.out.println("WARNING!\tMemory interface received invalid device parameters.");
        }
    }

    private int getAddress()
    {
        return Integer.parseInt(addressField.getText(), addressBinRadio.isSelected() ? 2 : 10);
    }

    private int getValue()
    {
        return Integer.parseInt(valueField.getText(), valueBinRadio.isSelected() ? 2 : 10);
    }

    private void storeWordInCurrentMemoryModule()  // TODO : Add second value word field when 64-bit cache is selected
    {
        try
        {
            int[] newValueS = new int[] { getValue() };
            if(lineRadio.isSelected())
            {
                newValueS = new int[currentlySelected.getLineSize()];
                for(int i = 0; i < newValueS.length; i++)
                {
                    newValueS[i] = getValue();
                }
            }
            currentlySelected.store(new MemoryRequest(-1, REQUEST_TYPE.STORE,
                                                      new Object[] { getAddress(), newValueS }));
        }
        catch(NumberFormatException e)
        {
            System.out.println("WARNING!\tMemory interface received invalid store parameters.");
        }
        displayText.setText(currentlySelected.getMemoryDisplay());
    }

    private void loadCurrentMemoryModule()
    {
        try
        {
            int[] line = currentlySelected.load(new MemoryRequest(-1, REQUEST_TYPE.LOAD,
                                                new Object[] { getAddress(), lineRadio.isSelected() }));
            returnField.setText("");
            StringBuilder newText = new StringBuilder();
            for(int word : line)
            {
                newText.append(word);
                newText.append('\t');
            }
            newText.deleteCharAt(newText.length() - 1);
            returnField.setText(newText.toString());
        }
        catch(NumberFormatException e)
        {
            System.out.println("WARNING!\tMemory interface received invalid load parameters.");
        }
        displayText.setText(currentlySelected.getMemoryDisplay());
    }
}
