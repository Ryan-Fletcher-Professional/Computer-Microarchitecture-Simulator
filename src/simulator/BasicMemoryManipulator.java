package simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static main.GLOBALS.*;
import memory.MemoryModule;
import memory.MemoryRequest;
import memory.RegisterBankForTesting;

public class BasicMemoryManipulator extends JFrame
{
    private static final Logger logger = Logger.getLogger(BasicMemoryManipulator.class.getName());

    private final int id;
    private RegisterBankForTesting[] registerBanks;
    private int frameWidth, frameHeight;

    private JTextField addressField, valueField, columnSizeField, lineSizeField, cacheField, ramField;
    private JScrollPane memoryDisplayPane, registerDisplayPane, reversalDisplayPane;
    private JTextArea memoryDisplayText, registerDisplayText, reversalDisplayText;
    private JRadioButton cacheRadio, ramRadio, dataRadio, instructionRadio, shortWordsRadio, longWordsRadio,
                         wordRadio, lineRadio, addressBinRadio, addressDecRadio, valueBinRadio, valueDecRadio;
    private JList<MemoryModule> instructionCachesList, dataCachesList, unifiedMemoryList;
    private JList<MemoryModule>[] memoryLists;
    private DefaultListModel<MemoryModule> instructionCachesModel, dataCachesModel, unifiedMemoryModel;
    private MemoryModule currentlySelected;

    public BasicMemoryManipulator(int id, RegisterBankForTesting[] registerBanks)
    {
        this.id = id;
        new BasicMemoryManipulator(id, registerBanks, DEFAULT_UI_WIDTH, DEFAULT_UI_HEIGHT);
    }

    public BasicMemoryManipulator(int id, RegisterBankForTesting[] registerBanks, int width, int height)
    {
        this.id = id;
        this.registerBanks = registerBanks;
        this.frameWidth = width;
        this.frameHeight = height;

        setTitle("Basic Memory Manipulator");
        setSize(width, height);
        setLayout(new BorderLayout());
        setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);

        // Toolbar at the top
        JToolBar toolBar = new JToolBar();
        JLabel countLabel = new JLabel("Cycles: 0");
        countLabel.setMinimumSize(new Dimension(200, 30));
        countLabel.setMaximumSize(new Dimension(200, 30));
        JButton tickButton = new JButton("Cycle Clock");
        tickButton.setMinimumSize(new Dimension(100, 30));
        tickButton.setMaximumSize(new Dimension(100, 30));
        JTextField tickField = new JTextField(1);
        tickField.setMinimumSize(new Dimension(100, 30));
        tickField.setMaximumSize(new Dimension(100, 30));
        tickButton.addActionListener(e -> {
            int numTicks = 1;
            try{ numTicks = Integer.parseInt(tickField.getText()); }
            catch(NumberFormatException _ignored_) {}
            for(int i = 0; i < numTicks; i++)
            {
                CURRENT_TICK += 1;
                for(JList<MemoryModule> list : memoryLists)
                {
                    for(int j = 0; j < list.getModel().getSize(); j++)
                    {
                        list.getModel().getElementAt(j).tick();
                    }
                }
            }
            countLabel.setText("Cycles: " + CURRENT_TICK);
            updateDisplay();
        });
        JButton resetButton = new JButton("RESET");
        resetButton.setMinimumSize(new Dimension(100, 30));
        resetButton.setMaximumSize(new Dimension(100, 30));
        resetButton.addActionListener(e -> {
            setVisible(false);
            new BasicMemoryManipulator(GET_ID(), registerBanks);
            for(RegisterBankForTesting bank : registerBanks)
            {
                if(bank != null) { bank.reset(); }
            }
        });
        toolBar.add(countLabel);
        toolBar.add(tickButton);
        toolBar.add(tickField);
        JPanel blankPanel = new JPanel();
        Dimension fill = new Dimension(width - 300, 30);
        blankPanel.setMinimumSize(fill);
        blankPanel.setMaximumSize(fill);
        toolBar.add(blankPanel);
        toolBar.add(resetButton, BorderLayout.EAST);
        add(toolBar, BorderLayout.NORTH);

        // Top left for control, top right for cache configuration view, bottom for state view
        JSplitPane topBottomPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        topBottomPane.setDividerLocation(height / 2);
        JSplitPane leftRightPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftRightPane.setDividerLocation(width / 2);

        // Left
        JPanel leftPanel = new JPanel(new GridLayout(0, 1));

        // Memory manipulation
        JPanel memoryOperationsPanel = new JPanel(new GridLayout(0, 1));  // 0 rows means "as many as needed"

        JPanel entryPanel = new JPanel( new GridLayout(3, 2));
        addressField = new JTextField(1);
        valueField = new JTextField(1);
        JPanel addressRadioPanel = new JPanel(new GridLayout(1, 2));
        addressBinRadio = new JRadioButton("Binary");
        addressDecRadio = new JRadioButton("Decimal");
        ButtonGroup addressGroup = new ButtonGroup();
        addressGroup.add(addressBinRadio);
        addressGroup.add(addressDecRadio);
        addressBinRadio.setSelected(true);
        addressBinRadio.addActionListener(e -> { if(currentlySelected != null){ updateDisplay(); } });
        addressDecRadio.addActionListener(e -> { if(currentlySelected != null){ updateDisplay(); } });
        addressRadioPanel.add(addressBinRadio);
        addressRadioPanel.add(addressDecRadio);
        JPanel valueRadioPanel = new JPanel(new GridLayout(1, 2));
        valueBinRadio = new JRadioButton("Binary");
        valueDecRadio = new JRadioButton("Decimal");
        ButtonGroup valueGroup = new ButtonGroup();
        valueGroup.add(valueBinRadio);
        valueGroup.add(valueDecRadio);
        valueBinRadio.setSelected(true);
        valueBinRadio.addActionListener(e -> { if(currentlySelected != null){ updateDisplay(); } });
        valueDecRadio.addActionListener(e -> { if(currentlySelected != null){ updateDisplay(); } });
        valueRadioPanel.add(valueBinRadio);
        valueRadioPanel.add(valueDecRadio);
        entryPanel.add(new JLabel("Address"));
        entryPanel.add(new JLabel("Argument"));
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
        storeButton.addActionListener(e -> storeInCurrentMemoryModule());
        loadButton.addActionListener(e -> loadFromCurrentMemoryModule());
        buttonPanel.add(storeButton);
        buttonPanel.add(loadButton);

        memoryOperationsPanel.add(entryPanel);
        memoryOperationsPanel.add(wordPanel);
        memoryOperationsPanel.add(buttonPanel);

        // MemoryModule creation
        JPanel memoryCreationPanel = new JPanel(new GridLayout(0, 2));

        JPanel cachePanel = new JPanel(new GridLayout(1, 2));
        cacheRadio = new JRadioButton("Cache");
        cacheField = new JTextField();
        cachePanel.add(cacheField);
        cachePanel.add(cacheRadio);
        JPanel ramPanel = new JPanel(new GridLayout(1, 2));
        ramRadio = new JRadioButton("RAM");
        ramField = new JTextField();
        ramPanel.add(ramField);
        ramPanel.add(ramRadio);
        ButtonGroup memoryKindSelection = new ButtonGroup();
        memoryKindSelection.add(cacheRadio);
        memoryKindSelection.add(ramRadio);
        ramRadio.setSelected(true);

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

        memoryCreationPanel.add(cachePanel);
        memoryCreationPanel.add(ramPanel);
        memoryCreationPanel.add(dataRadio);
        memoryCreationPanel.add(instructionRadio);
        memoryCreationPanel.add(columnSizePanel);
        memoryCreationPanel.add(lineSizePanel);
        memoryCreationPanel.add(shortWordsRadio);
        memoryCreationPanel.add(longWordsRadio);

        recurBackgroundColor(memoryCreationPanel, new Color(200, 200, 200));

        leftPanel.add(memoryOperationsPanel);
        leftPanel.add(memoryCreationPanel);

        // Right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        unifiedMemoryModel = new DefaultListModel<>();
        dataCachesModel = new DefaultListModel<>();
        instructionCachesModel = new DefaultListModel<>();
        unifiedMemoryList = new JList<>(unifiedMemoryModel);
        dataCachesList = new JList<>(dataCachesModel);
        instructionCachesList = new JList<>(instructionCachesModel);
        memoryLists = new JList[] { unifiedMemoryList, dataCachesList, instructionCachesList };

        JPanel memoryModificationPanel = new JPanel(new GridLayout(1, 0));
        JButton throughNoAllocateButton = new JButton("Set Write-Through No Allocate");
        JButton throughAllocateButton = new JButton("Set Write-Through Allocate");
        JButton writeBackButton = new JButton("Set Write-Back");
        throughNoAllocateButton.addActionListener(e -> {
                if(currentlySelected != null)
                {
                    currentlySelected.setWriteMode(WRITE_MODE.THROUGH_NO_ALLOCATE);
                    updateDisplay();
                }
            });
        throughAllocateButton.addActionListener(e -> {
                if(currentlySelected != null)
                {
                    currentlySelected.setWriteMode(WRITE_MODE.THROUGH_ALLOCATE);
                    updateDisplay();
                }
            });
        writeBackButton.addActionListener(e -> {
                if(currentlySelected != null)
                {
                    currentlySelected.setWriteMode(WRITE_MODE.BACK);
                    updateDisplay();
                }
            });
        memoryModificationPanel.add(throughNoAllocateButton);
        memoryModificationPanel.add(throughAllocateButton);
        memoryModificationPanel.add(writeBackButton);

        rightPanel.add(createListPanel("Unified Memory", unifiedMemoryList,
                                                              new JList[] {instructionCachesList, dataCachesList}));
        rightPanel.add(createListPanel("Data Caches", dataCachesList,
                                                           new JList[] {instructionCachesList, unifiedMemoryList}));
        rightPanel.add(createListPanel("Instruction Caches", instructionCachesList,
                                                                  new JList[] {dataCachesList, unifiedMemoryList}));
        rightPanel.add(memoryModificationPanel);

        // Bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        registerDisplayText = new JTextArea();
        registerDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        registerDisplayText.setMinimumSize(new Dimension(0, 90));
        registerDisplayText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        registerDisplayText.setEditable(false);
        memoryDisplayText = new JTextArea();
        memoryDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        memoryDisplayText.setEditable(false);
        registerDisplayPane = new JScrollPane(registerDisplayText);
        registerDisplayPane.setPreferredSize(new Dimension(frameWidth, 90));
        bottomPanel.add(registerDisplayPane, BorderLayout.NORTH);
        memoryDisplayPane = new JScrollPane(memoryDisplayText);
        bottomPanel.add(memoryDisplayPane, BorderLayout.CENTER);

        // Finish arranging window
        leftRightPane.setLeftComponent(leftPanel);
        leftRightPane.setRightComponent(rightPanel);

        topBottomPane.setTopComponent(leftRightPane);
        topBottomPane.setBottomComponent(bottomPanel);

        this.add(topBottomPane, BorderLayout.CENTER);

        addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent evt) {
                Component c = (Component)evt.getSource();
                Dimension newSize = c.getSize();
                frameWidth = newSize.width;
                frameHeight = newSize.height;
                Dimension fill = new Dimension(frameWidth - 500, 30);
                blankPanel.setMinimumSize(fill);
                blankPanel.setMaximumSize(fill);
            }
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}
            @Override
            public void componentHidden(ComponentEvent e) {}
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        updateDisplay();
    }

    /**
     * Helper method to recursively set the background color of current and all its component elements except text areas
     *  and fields.
     * @param current The topmost component to have its background color set.
     * @param color The new background color.
     */
    private static void recurBackgroundColor(Component current, Color color)
    {
        current.setBackground(color);
        if(current instanceof Container)
        {
            for(Component child : ((Container)current).getComponents())
            {
                if(!(child instanceof JTextField) && !(child instanceof JTextArea))
                {
                    recurBackgroundColor(child, color);
                }
            }
        }
    }

    /**
     * Forces various components to retrieve most recent display info.
     */
    private void updateDisplay()
    {
        JScrollBar hBarReg = registerDisplayPane.getHorizontalScrollBar();
        int xR = hBarReg.getValue();
        JScrollBar vBarReg = registerDisplayPane.getVerticalScrollBar();
        int yR = vBarReg.getValue();
        JScrollBar hBarMem = memoryDisplayPane.getHorizontalScrollBar();
        int xM = hBarMem.getValue();
        JScrollBar vBarMem = memoryDisplayPane.getVerticalScrollBar();
        int yM = vBarMem.getValue();

        registerDisplayText.setText(registerBanks[INDEXABLE_BANK_INDEX].getDisplayText(8));
        if(currentlySelected != null)
            { memoryDisplayText.setText(currentlySelected.getMemoryDisplay(getRadices()[0], getRadices()[1])); }

        SwingUtilities.invokeLater(() -> {
            hBarReg.setValue(xR);
            vBarReg.setValue(yR);
            hBarMem.setValue(xM);
            vBarMem.setValue(yM);
            for(JList<MemoryModule> list : memoryLists)
            {
                if(list.getModel().getSize() > 0)
                {                                                     // Forces JLists to update item names
                    ((DefaultListModel<MemoryModule>)list.getModel()).setElementAt(
                                                     list.getModel().getElementAt(0), 0);
                }
            }
        });
    }

    /**
     * For creating the memory device lists.
     * @param title Unified/Data/Instruction
     * @param list This one.
     * @param otherLists The other lists. Needed for reference storage.
     * @return JPanel containing the list named, a button to trigger adding items to the list, and the list display.
     */
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
                updateDisplay();
            }
        });

        return panel;
    }

    /**
     * Helper method for createListPanel.
     * @param list The list to which the new MemoryModule will be appended.
     */
    private void createNewMemoryModule(JList<MemoryModule> list)
    {
        DefaultListModel<MemoryModule> model = (DefaultListModel<MemoryModule>)list.getModel();
        MemoryModule newModule;
        int cacheDelay = DEFAULT_CACHE_ACCESS_DELAY;
        int ramDelay = DEFAULT_RAM_ACCESS_DELAY;
        try { cacheDelay = Integer.parseInt(cacheField.getText()); } catch(NumberFormatException _ignored_) {}
        try { ramDelay = Integer.parseInt(ramField.getText()); } catch(NumberFormatException _ignored_) {}

        try
        {
            MEMORY_KIND kind = cacheRadio.isSelected() ? MEMORY_KIND.CACHE : MEMORY_KIND.RAM;
            newModule = new MemoryModule(GET_ID(),
                                         kind,
                                         dataRadio.isSelected() ? MEMORY_TYPE.DATA : MEMORY_TYPE.INSTRUCTION,
                                         shortWordsRadio.isSelected() ? WORD_LENGTH.SHORT : WORD_LENGTH.LONG,
                                         kind.equals(MEMORY_KIND.CACHE) ? DEFAULT_CACHE_WRITE_MODE : DEFAULT_RAM_WRITE_MODE,
                                         model.getSize() > 0 ? model.getElementAt(model.getSize() - 1) :
                                                 (unifiedMemoryModel.getSize() > 0 ?
                                                  unifiedMemoryModel.getElementAt(unifiedMemoryModel.getSize() - 1) :
                                                  null),
                                         Integer.parseInt(columnSizeField.getText()),
                                         Integer.parseInt(lineSizeField.getText()),
                                         cacheRadio.isSelected() ? cacheDelay : ramDelay);
            model.addElement(newModule);
        }
        catch(NumberFormatException e)
        {
            WARN("Memory interface received invalid device parameters.");
        }
    }

    /**
     * @return Numerical content of address text field.
     */
    private int getAddress()
    {
                    // Must parse as long so that 32-character inputs are accepted
        return (int)Long.parseLong(addressField.getText(), addressBinRadio.isSelected() ? 2 : 10);
    }

    /**
     * @return true iff the contents of the argument text field represent a numbered register rather than a value.
     */
    private boolean valueIsGeneralRegister()
    {
        return valueField.getText().charAt(0) == 'R';
    }

    /**
     * @return true iff the contents of the argument text field represent a named register rather than a value.
     */
    private boolean valueIsInternalRegister()
    {
        return Arrays.asList(INTERNAL_REGISTER_NAMES).contains(valueField.getText());
    }

    /**
     * @return true iff the contents of the argument text field represent a register rather than a value.
     */
    private boolean valueIsRegister()
    {
        return valueIsGeneralRegister() || valueIsInternalRegister();
    }

    /**
     * @return Numerical content of the argument text field.
     *         If it's a register, returns that register's index in its respective bank.
     */
    private int getValue()
    {
        String text = valueField.getText();
        int radix = 10;
        try
        {
            if(valueIsGeneralRegister())
            {
                text = text.substring(1);
            }
            else if(valueIsInternalRegister())
            {
                return Arrays.asList(INTERNAL_REGISTER_NAMES).indexOf(text);
            }
            else if(valueBinRadio.isSelected())
            {
                radix = 2;
            }
        }
        catch(StringIndexOutOfBoundsException e) { throw new NumberFormatException(); }

        return (int)Long.parseLong(text, radix);
    }

    /**
     * @return [ Selected numerical base for addresses, Selected numerical base for arguments ]
     */
    private int[] getRadices()
    {
        return new int[] { addressBinRadio.isSelected() ? 2 : 10, valueBinRadio.isSelected() ? 2 : 10 };
    }

    /**
     * Stores value in argument text field or corresponding register to given address in selected MemoryModule.
     */
    private void storeInCurrentMemoryModule()  // TODO : Add second value word field when 64-bit cache is selected
    {
        try
        {
            if(currentlySelected == null) { throw new NumberFormatException(); }

            int[] newValueS = new int[] { getValue() };

            RegisterBankForTesting bank = registerBanks[INDEXABLE_BANK_INDEX];
            if(valueIsRegister())
            {
                int register = newValueS[0];
                if(newValueS[0] > 15)
                {
                    bank = registerBanks[INTERNAL_BANK_INDEX];
                    newValueS[0] -= 15;
                }
                if(lineRadio.isSelected()) { newValueS = new int[currentlySelected.getLineSize()]; }
                for(int i = 0; i < newValueS.length; i++)
                {
                    newValueS[i] = (int)bank.load(register + i);
                }
            }

            LinkedList<MemoryRequest> request = new LinkedList<>(List.of(
                                                new MemoryRequest(valueIsRegister() ? bank.getID() : -1,
                                                                  currentlySelected.getID(),
                                                                  dataRadio.isSelected() ? MEMORY_TYPE.DATA : MEMORY_TYPE.INSTRUCTION,
                                                                  REQUEST_TYPE.STORE,
                                                                  new Object[]{getAddress(), newValueS})));
            currentlySelected.store(request);
        }
        catch(NumberFormatException e)
        {
            WARN("Memory interface received invalid store parameters.");
        }
        updateDisplay();
    }

    /**
     * Loads value from selected MemoryModule into register in argument text field.
     */
    private void loadFromCurrentMemoryModule()
    {
        try
        {
            if(currentlySelected == null) { throw new NumberFormatException(); }

            int register = getValue();
            RegisterBankForTesting bank = registerBanks[INDEXABLE_BANK_INDEX];
            if(register > 15)
            {
                bank = registerBanks[INTERNAL_BANK_INDEX];
                register -= 15;
            }

            LinkedList<MemoryRequest> request = new LinkedList<>(List.of(
                                                new MemoryRequest(bank.getID(), currentlySelected.getID(),
                                                                  currentlySelected.getType(), REQUEST_TYPE.LOAD,
                                                                  new Object[]{getAddress(), lineRadio.isSelected()})));
            int[] line = currentlySelected.load(request);

            for(int i = 0; i < line.length; i++)
            {
                bank.store(register + i, line[i]);
            }

            registerDisplayText.setText(bank.getDisplayText(8));
        }
        catch(NumberFormatException e)
        {
            WARN("Memory interface received invalid load parameters.");
        }

        updateDisplay();
    }

    /**
     * @return GET_TRACE_LINE(String invoker, int offset) invoked by the method below this one on the stack.
     */
    private static String GET_TRACE_LINE()
    {
        return GET_TRACE_LINE(Thread.currentThread().getStackTrace()[2].getMethodName(), 1);
    }

    /**
     * @param invoker The name of the method containing the line in question.
     * @param offset N - 1, where N is the number of method calls between this and invoker.
     * @return {tab}at className.invoker(className.java:lineNumber)
     */
    private static String GET_TRACE_LINE(String invoker, int offset)
    {
        String className = MethodHandles.lookup().lookupClass().getName();
        return "\tat " + className + "." + invoker + "(" + className + ".java:" +
               Thread.currentThread().getStackTrace()[2 + offset].getLineNumber() + ")";
    }

    /**
     * Prints a red warning message to the console, like an Exception but it doesn't cause terminal and can't be caught.
     * {tab}at className.invoker(className.java:lineNumber) message
     * @param message The message to follow the logistical info in the warning.
     */
    private static void WARN(String message)
    {
        logger.log(Level.WARNING,
                  GET_TRACE_LINE(Thread.currentThread().getStackTrace()[2].getMethodName(), 1) +
                      " " + message);
    }
}
