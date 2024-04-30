package main;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static main.Assembler.*;
import static main.GLOBALS.*;

import static instructions.Instructions.*;

import instructions.Instruction;
import memory.MemoryModule;
import memory.MemoryRequest;
import memory.RegisterFileModule;
import pipeline.Pipeline;

public class Simulator extends JFrame
{
    private static final Logger logger = Logger.getLogger(Simulator.class.getName());

    private final int id, startingPC, numSpecialInstructions;
    private JSplitPane topBottomPane;
    private JLabel callStackLabel, reversalStackLabel, pipelineLabel, pipelineOutputLabel;
    private RegisterFileModule[] registerBanks;
    private Pipeline pipeline;
    private int frameWidth, frameHeight;

    private JTextField addressField, valueField, columnSizeField, lineSizeField, cacheField, ramField;
    private JScrollPane callDisplayPane, reversalDisplayPane, pipelineDisplayPane,
                        currentlyVisibleBank, currentlyInvisibleBank;
    private JScrollPane[] panes;
    private JPanel[] stackPipeline;
    private int currentStackPipelineIndex;
    private JTextArea memoryDisplayText, indexableBankDisplayText, internalBankDisplayText,
                      callStackDisplayText, reversalStackDisplayText, pipelineDisplayText;
    private JRadioButton cacheRadio, ramRadio, dataRadio, instructionRadio, shortWordsRadio, longWordsRadio,
                         wordRadio, lineRadio, addressBinRadio, addressDecRadio, addressHexRadio,
                         valueBinRadio, valueDecRadio, valueHexRadio;
    private JList<MemoryModule>[] memoryLists;
    private DefaultListModel<MemoryModule> unifiedMemoryModel, instructionCachesModel, dataCachesModel;
    private MemoryModule currentlySelectedMemory;
    private JPanel currentlyVisibleControls, currentlyInvisibleControls, stackPipelinePanel,
                   callStackDisplayPanel, reversalStackDisplayPanel, pipelineDisplayPanel, pipelineLabelPanel;
    private JCheckBox activePipelineCheckbox;
    public int stalls;
    public JLabel stallsLabel;
    public int noops;
    public JLabel noopsLabel;

    public Simulator(int id, RegisterFileModule[] registerBanks, Pipeline pipeline, int extendedState, int startingPC, int[][][] startingMemories, int numSpecialInstructions)
    {
        this(id, registerBanks, pipeline, DEFAULT_UI_WIDTH, DEFAULT_UI_HEIGHT, extendedState, startingPC, startingMemories, numSpecialInstructions);
    }

    public Simulator(int id, RegisterFileModule[] registerBanks, Pipeline pipeline, int width, int height,
                     int extendedState, int startingPC, int[][][] startingMemories, int numSpecialInstructions)
    {
        this.id = id;
        this.registerBanks = registerBanks;
        this.pipeline = pipeline;
        this.frameWidth = width;
        this.frameHeight = height;
        this.startingPC = startingPC;
        this.numSpecialInstructions = numSpecialInstructions;

        setTitle("Basic Memory Manipulator");
        setSize(width, height);
        setLayout(new BorderLayout());
        setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);

        pipeline.setSimulator(this);

        // Toolbar at the top
        JPanel toolBarPanel = new JPanel(new BorderLayout());
        JToolBar toolBar = new JToolBar();
        stallsLabel = new JLabel("Stalls: 0");
        stallsLabel.setMinimumSize(new Dimension(150, 30));
        stallsLabel.setMaximumSize(new Dimension(150, 30));
        noopsLabel = new JLabel("No-Ops: 0");
        noopsLabel.setMinimumSize(new Dimension(150, 30));
        noopsLabel.setMaximumSize(new Dimension(150, 30));
        JLabel countLabel = new JLabel("Cycles: 0");
        countLabel.setMinimumSize(new Dimension(200, 30));
        countLabel.setMaximumSize(new Dimension(200, 30));
        JButton tickButton = new JButton("Cycle");
        tickButton.setMinimumSize(new Dimension(100, 30));
        tickButton.setMaximumSize(new Dimension(100, 30));
        JTextField tickField = new JTextField(1);
        tickField.setMinimumSize(new Dimension(100, 30));
        tickField.setMaximumSize(new Dimension(100, 30));
        tickButton.addActionListener(e -> {
            Instruction output = null;
            double numTicks = Double.POSITIVE_INFINITY;
            try{ numTicks = Integer.parseInt(tickField.getText()); }
            catch(NumberFormatException _ignored_) {}
            for(int i = 0; i < numTicks; i++)
            {
                output = null;
                boolean aboutToHalt = false;
                boolean doneOnce = false;
                while((output == null) || !(AUX_EQUALS(output.getAuxBits(AUX_FETCHED), AUX_TRUE)))
                {
                    CURRENT_TICK += 1;
                    if((CURRENT_TICK % PRINT_CHECKPOINT_INDEX) == 0)
                    {
                        System.out.println("CYCLE: " + String.format("%,d", CURRENT_TICK));
                        for(String line : registerBanks[INDEXABLE_BANK_INDEX].getDisplayText(10).split("\n"))
                        {
                            System.out.println("\t" + line);
                        }
                    }
                    aboutToHalt = pipeline.preExecute();  // Happens before memory cycled, so memory cycling can be "in-line" with pipeline cycling
                    for(JList<MemoryModule> list : memoryLists)
                    {
                        for(int j = 0; j < list.getModel().getSize(); j++)
                        {
                            list.getModel().getElementAt(j).tick();
                        }
                    }
                    if(aboutToHalt)
                    {
                        System.out.println("HALT ENCOUNTERED");
                        pipeline.openWrite();
                        break;
                    }
                    output = pipeline.execute(activePipelineCheckbox.isSelected() || !doneOnce);
                    if(ERROR_INSTRUCTIONS.contains(output.getHeader()))
                    {
                        System.out.println("ERROR ENCOUNTERED: " + output.word.toString());
                        break;
                    }

                    doneOnce = true;
                    if(activePipelineCheckbox.isSelected()) { break; }
                }
                if(aboutToHalt)
                {
                    break;
                }
            }
            countLabel.setText("Cycles: " + String.format("%,d", CURRENT_TICK));
            if(output != null)
            {
                long pc = (output.getAuxBits(AUX_PC_AT_FETCH) == null) ? -1 : output.getAuxBits(AUX_PC_AT_FETCH).toInt();
                pipelineOutputLabel.setText("Line " + ((pc == -1L) ? "--" : ((output.wordLength() == WORD_SIZE_SHORT ? SHORT_INSTRUCTION_ADDRESS_UNFIX(pc, numSpecialInstructions) : LONG_INSTRUCTION_ADDRESS_UNFIX(pc, numSpecialInstructions)))) +
                                            ": " + ((MNEMONICS.get(output.getHeader()) != null) ? MNEMONICS.get(output.getHeader()) : INTERNAL_MNEMONICS.get(output.getHeader())));
            }
            updateDisplay();
        });

        stackPipelinePanel = new JPanel();
        JButton stackPipelineToggle = new JButton("Rotate Stack/Pipeline View");
        stackPipelineToggle.setMinimumSize(new Dimension(300, 30));
        stackPipelineToggle.setMaximumSize(new Dimension(300, 30));
        stackPipelineToggle.addActionListener(e -> {
            stackPipelinePanel.remove(stackPipeline[currentStackPipelineIndex]);
            currentStackPipelineIndex = (currentStackPipelineIndex + 1) % stackPipeline.length;
            stackPipelinePanel.add(stackPipeline[currentStackPipelineIndex]);
            updateDisplay();
        });
        JPanel memoryPanel = new JPanel(new GridLayout(0, 1));
        JButton controlsToggle = new JButton("Toggle Controls View");
        controlsToggle.setMinimumSize(new Dimension(300, 30));
        controlsToggle.setMaximumSize(new Dimension(300, 30));
        controlsToggle.addActionListener(e -> {
            memoryPanel.remove(currentlyVisibleControls);
            memoryPanel.add(currentlyInvisibleControls);
            JPanel temp = currentlyVisibleControls;
            currentlyVisibleControls = currentlyInvisibleControls;
            currentlyInvisibleControls = temp;
            updateDisplay();
        });
        JPanel bankPanel = new JPanel(new GridLayout(1, 1));
        JButton saveButton = new JButton("Save to File");
        saveButton.setMinimumSize(new Dimension(300, 30));
        saveButton.setMaximumSize(new Dimension(300, 30));
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a path to save");
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir") + "/" + PATH_TO_FILES));
            fileChooser.setSelectedFile(new File(this.id + ".txt"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                currentlySelectedMemory.dumpToFile(fileToSave.getAbsolutePath());
            }
        });

        JButton stepBackButton = new JButton("UNDO");
        stepBackButton.setMinimumSize(new Dimension(100, 30));
        stepBackButton.setMaximumSize(new Dimension(100, 30));
        JTextField quantityField = new JTextField(1);
        quantityField.setMinimumSize(new Dimension(100, 30));
        quantityField.setMaximumSize(new Dimension(100, 30));
        JTextField skipField = new JTextField(1);
        skipField.setMinimumSize(new Dimension(100, 30));
        skipField.setMaximumSize(new Dimension(100, 30));
        stepBackButton.addActionListener(e -> {
            pipeline.fakeUndo(Integer.parseInt(quantityField.getText()), Integer.parseInt(skipField.getText()));
            updateDisplay();
        });

        JToolBar resetBar = new JToolBar();
        JButton resetButton = new JButton("RESET");
        resetButton.setMinimumSize(new Dimension(100, 30));
        resetButton.setMaximumSize(new Dimension(100, 30));
        resetButton.setPreferredSize(new Dimension(100, 30));
        resetButton.addActionListener(e -> {
            setVisible(false);
//            new Simulator(GET_ID(), registerBanks, pipeline, this.getExtendedState(), startingPC, startingMemories, numSpecialInstructions);
//            for(RegisterFileModule bank : registerBanks)
//            {
//                if(bank != null) { bank.reset(); }
//            }
//            registerBanks[INTERNAL_BANK_INDEX].store(PC_INDEX, startingPC);
//            pipeline.reset();
            currentId = 0;
            CURRENT_TICK = 0;
            Main.main(null);  // Not a word!
        });
        Component[] toolBarComponents = new Component[] { stallsLabel, noopsLabel, countLabel, tickButton, tickField, Box.createHorizontalStrut(30),
                                                          stackPipelineToggle, controlsToggle, Box.createHorizontalStrut(30),
                                                          stepBackButton, quantityField, skipField, Box.createHorizontalStrut(30),
                                                          saveButton };
        for(Component component : toolBarComponents)
        {
            toolBar.add(component);
        }
        //toolBar.add(Box.createHorizontalGlue());
        resetBar.add(resetButton, BorderLayout.EAST);
        toolBarPanel.add(toolBar, BorderLayout.CENTER);
        toolBarPanel.add(resetBar, BorderLayout.EAST);
        add(toolBarPanel, BorderLayout.NORTH);

        // Top left for control, top right for cache configuration view, bottom for state view
        topBottomPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        topBottomPane.setDividerLocation((height / 2) - 100);
        JSplitPane leftRightPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        leftRightPane.setDividerLocation(width / 2);

        // Left
        JPanel leftPanel = new JPanel(new GridLayout(1, 2));

        // Stacks
        callStackDisplayPanel = new JPanel(new BorderLayout());
        callStackLabel = new JLabel("Call Stack");
        callStackLabel.setMinimumSize(new Dimension(200, 30));
        callStackLabel.setMaximumSize(new Dimension(200, 30));
        callStackDisplayText = new JTextArea();
        callStackDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        callStackDisplayText.setEditable(false);
        callDisplayPane = new JScrollPane(callStackDisplayText);
        callStackDisplayPanel.add(callStackLabel, BorderLayout.NORTH);
        callStackDisplayPanel.add(callDisplayPane, BorderLayout.SOUTH);

        reversalStackDisplayPanel = new JPanel(new BorderLayout());
        reversalStackLabel = new JLabel("Reversal Stack");
        reversalStackLabel.setMinimumSize(new Dimension(200, 30));
        reversalStackLabel.setMaximumSize(new Dimension(200, 30));
        reversalStackDisplayText = new JTextArea();
        reversalStackDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reversalStackDisplayText.setEditable(false);
        reversalDisplayPane = new JScrollPane(reversalStackDisplayText);
        reversalStackDisplayPanel.add(reversalStackLabel, BorderLayout.NORTH);
        reversalStackDisplayPanel.add(reversalDisplayPane, BorderLayout.SOUTH);

        // Pipeline
        pipelineDisplayPanel = new JPanel(new BorderLayout());
        pipelineLabelPanel = new JPanel(new GridLayout(2, 1));
        JPanel pipelineTopPanel = new JPanel();
        pipelineLabel = new JLabel("Pipeline");
        pipelineLabel.setMinimumSize(new Dimension(200, 30));
        pipelineLabel.setMaximumSize(new Dimension(200, 30));
        activePipelineCheckbox = new JCheckBox();
        activePipelineCheckbox.setSelected(true);
        pipelineTopPanel.add(pipelineLabel);
        pipelineTopPanel.add(activePipelineCheckbox);
        pipelineOutputLabel = new JLabel("Line --: ----");
        pipelineLabelPanel.add(pipelineTopPanel);
        pipelineLabelPanel.add(pipelineOutputLabel);
        pipelineDisplayText = new JTextArea();
        pipelineDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        pipelineDisplayText.setEditable(false);
        pipelineDisplayPane = new JScrollPane(pipelineDisplayText);
        pipelineDisplayPanel.add(pipelineLabelPanel, BorderLayout.NORTH);
        pipelineDisplayPanel.add(pipelineDisplayPane, BorderLayout.SOUTH);

        stackPipeline = new JPanel[] { callStackDisplayPanel, reversalStackDisplayPanel, pipelineDisplayPanel };
        currentStackPipelineIndex = 0;
        stackPipelinePanel.add(stackPipeline[currentStackPipelineIndex]);

        // Memory
        // Memory manipulation
        JPanel memoryOperationsPanel = new JPanel(new GridLayout(0, 1));  // 0 rows means "as many as needed"
        JPanel entryPanel = new JPanel( new GridLayout(3, 2));
        JPanel addressPanel = new JPanel(new BorderLayout());
        addressField = new JTextField(1);
        addressPanel.add(addressField, BorderLayout.NORTH);
        JPanel valuePanel = new JPanel(new BorderLayout());
        valueField = new JTextField(1);
        valuePanel.add(valueField, BorderLayout.NORTH);
        valueField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JPanel addressRadioPanel = new JPanel(new GridLayout(1, 2));
        addressBinRadio = new JRadioButton("Bin");
        addressDecRadio = new JRadioButton("Dec");
        addressHexRadio = new JRadioButton("Hex");
        ButtonGroup addressGroup = new ButtonGroup();
        addressGroup.add(addressBinRadio);
        addressGroup.add(addressDecRadio);
        addressGroup.add(addressHexRadio);
        addressBinRadio.setSelected(true);
        addressBinRadio.addActionListener(e -> { updateDisplay(); });
        addressDecRadio.addActionListener(e -> { updateDisplay(); });
        addressHexRadio.addActionListener(e -> { updateDisplay(); });
        addressRadioPanel.add(addressBinRadio);
        addressRadioPanel.add(addressDecRadio);
        addressRadioPanel.add(addressHexRadio);
        JPanel valueRadioPanel = new JPanel(new GridLayout(1, 2));
        valueBinRadio = new JRadioButton("Bin");
        valueDecRadio = new JRadioButton("Dec");
        valueHexRadio = new JRadioButton("Hex");
        ButtonGroup valueGroup = new ButtonGroup();
        valueGroup.add(valueBinRadio);
        valueGroup.add(valueDecRadio);
        valueGroup.add(valueHexRadio);
        valueHexRadio.setSelected(true);
        valueBinRadio.addActionListener(e -> { updateDisplay(); });
        valueDecRadio.addActionListener(e -> { updateDisplay(); });
        valueHexRadio.addActionListener(e -> { updateDisplay(); });
        valueRadioPanel.add(valueBinRadio);
        valueRadioPanel.add(valueDecRadio);
        valueRadioPanel.add(valueHexRadio);
        entryPanel.add(new JLabel("Address"));
        entryPanel.add(new JLabel("Argument"));
        entryPanel.add(addressPanel);
        entryPanel.add(valuePanel);
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

        //recurBackgroundColor(memoryCreationPanel, new Color(200, 200, 200));

        memoryPanel.add(memoryCreationPanel);
        currentlyVisibleControls = memoryCreationPanel;
        currentlyInvisibleControls = memoryOperationsPanel;

        leftPanel.add(stackPipelinePanel);
        leftPanel.add(memoryPanel);

        // Right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        unifiedMemoryModel = new DefaultListModel<>();
        dataCachesModel = new DefaultListModel<>();
        instructionCachesModel = new DefaultListModel<>();
        JList<MemoryModule> unifiedMemoryList = new JList<>(unifiedMemoryModel);
        JList<MemoryModule> dataCachesList = new JList<>(dataCachesModel);
        JList<MemoryModule> instructionCachesList = new JList<>(instructionCachesModel);
        memoryLists = new JList[] {unifiedMemoryList, dataCachesList, instructionCachesList};

        JPanel memoryModificationPanel = new JPanel(new GridLayout(1, 0));
        JButton throughNoAllocateButton = new JButton("Set Write-Through No Allocate");
        JButton throughAllocateButton = new JButton("Set Write-Through Allocate");
        JButton writeBackButton = new JButton("Set Write-Back");
        throughNoAllocateButton.addActionListener(e -> {
                if(currentlySelectedMemory != null)
                {
                    currentlySelectedMemory.setWriteMode(WRITE_MODE.THROUGH_NO_ALLOCATE);
                    updateDisplay();
                }
            });
        throughAllocateButton.addActionListener(e -> {
                if(currentlySelectedMemory != null)
                {
                    currentlySelectedMemory.setWriteMode(WRITE_MODE.THROUGH_ALLOCATE);
                    updateDisplay();
                }
            });
        writeBackButton.addActionListener(e -> {
                if(currentlySelectedMemory != null)
                {
                    currentlySelectedMemory.setWriteMode(WRITE_MODE.BACK);
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

        indexableBankDisplayText = new JTextArea();
        indexableBankDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        indexableBankDisplayText.setMinimumSize(new Dimension(0, 150));
        indexableBankDisplayText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        indexableBankDisplayText.setEditable(false);
        JScrollPane indexableDisplayPane = new JScrollPane(indexableBankDisplayText);
        indexableDisplayPane.setPreferredSize(new Dimension(frameWidth, 150));
        internalBankDisplayText = new JTextArea();
        internalBankDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        internalBankDisplayText.setMinimumSize(new Dimension(0, 90));
        internalBankDisplayText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        internalBankDisplayText.setEditable(false);
        JScrollPane internalDisplayPane = new JScrollPane(internalBankDisplayText);
        internalDisplayPane.setPreferredSize(new Dimension(frameWidth, 90));
        bankPanel.add(indexableDisplayPane);
        bankPanel.setPreferredSize(new Dimension(width, 150));
        currentlyVisibleBank = indexableDisplayPane;
        currentlyInvisibleBank = internalDisplayPane;

        memoryDisplayText = new JTextArea();
        memoryDisplayText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        memoryDisplayText.setEditable(false);

        bottomPanel.add(bankPanel, BorderLayout.NORTH);
        JScrollPane memoryDisplayPane = new JScrollPane(memoryDisplayText);
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
            }
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}
            @Override
            public void componentHidden(ComponentEvent e) {}
        });

        panes = new JScrollPane[] {memoryDisplayPane, indexableDisplayPane, internalDisplayPane, callDisplayPane, reversalDisplayPane };

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        setExtendedState(extendedState);

        if(startingMemories != null)
        {
            loadMemories(startingMemories);
            controlsToggle.doClick();
        }

        updateDisplay();
    }

    private boolean allAccessesAreEmpty()
    {
        for(JList<MemoryModule> list : memoryLists)
        {
            for(int i = 0; i < list.getModel().getSize(); i++)
            {
                if(list.getModel().getElementAt(i).getNumActiveAccesses() > 0) { return false; }
            }
        }
        return true;
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
        List<JScrollBar> bars = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for(JScrollPane pane : panes)
        {
            bars.add(pane.getHorizontalScrollBar());
            positions.add(bars.getLast().getValue());
            bars.add(pane.getVerticalScrollBar());
            positions.add(bars.getLast().getValue());
        }

        stallsLabel.setText("Stalls: " + String.format("%,d", stalls));
        noopsLabel.setText("No-Ops: " + String.format("%,d", noops));

        int radix = getRadices()[1];
        callStackDisplayText.setText(registerBanks[CALL_STACK_INDEX].getDisplayText(1, radix));
        reversalStackDisplayText.setText(registerBanks[REVERSAL_STACK_INDEX].getDisplayText(1, radix));
        pipelineDisplayText.setText(pipeline.getDisplayText((radix != 10) ? radix : 2));
        indexableBankDisplayText.setText(registerBanks[INDEXABLE_BANK_INDEX].getDisplayText(8, radix) +
                                             "\n\n" + registerBanks[INTERNAL_BANK_INDEX].getDisplayText(8, radix));
        //internalBankDisplayText.setText(registerBanks[INTERNAL_BANK_INDEX].getDisplayText(8, radix));
        if(currentlySelectedMemory != null)
            { memoryDisplayText.setText(currentlySelectedMemory.getMemoryDisplay(getRadices()[0], radix)); }

        Dimension paneSize = stackPipelinePanel.getSize();
        int dividerSize = ((BasicSplitPaneUI)topBottomPane.getUI()).getDivider().getDividerSize();
        paneSize.width = Math.min(stackPipelinePanel.getWidth(), 8 * (valueBinRadio.isSelected() ? 38 : (valueHexRadio.isSelected() ? 14 : 20)));
        callDisplayPane.setPreferredSize(new Dimension(paneSize.width, paneSize.height - dividerSize - callStackLabel.getHeight()));
        reversalDisplayPane.setPreferredSize(new Dimension(paneSize.width, paneSize.height - dividerSize - reversalStackLabel.getHeight()));
        pipelineDisplayPane.setPreferredSize(new Dimension(Math.max(paneSize.width, 8 * 38) * (int)((pipeline.getWordSize() == 64) ? 1.5 : 1), paneSize.height - (2 * dividerSize) - pipelineLabelPanel.getHeight()));

        SwingUtilities.invokeLater(() -> {
            for(int i = 0; i < bars.size(); i++)
            {
                bars.get(i).setValue(positions.get(i));
            }

            // Forces JLists to update item names
            for(JList<MemoryModule> list : memoryLists)
            {
                if(list.getModel().getSize() > 0)
                {
                    ((DefaultListModel<MemoryModule>)list.getModel()).setElementAt(
                                                     list.getModel().getElementAt(0), 0);
                }
            }

            validate();
            repaint();
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
                currentlySelectedMemory = list.getSelectedValue();
                for(JList other : otherLists)
                {
                    other.clearSelection();
                }
                updateDisplay();
            }
        });

        return panel;
    }

    private void loadMemories(int[][][] memories)
    {
        for(int[] unifiedModule : memories[0])
        {
            ramField.setText(Integer.toString(unifiedModule[0]));
            ramRadio.setSelected(true);
            dataRadio.setSelected(true);
            if(unifiedModule[3] == WORD_SIZE_LONG)
            {
                longWordsRadio.setSelected(true);
            }
            else
            {
                shortWordsRadio.setSelected(true);
            }
            columnSizeField.setText(Integer.toString(unifiedModule[1]));
            lineSizeField.setText(Integer.toString(unifiedModule[2]));
            createNewMemoryModule(memoryLists[0]);
        }
        for(int[] dataModule : memories[1])
        {
            cacheField.setText(Integer.toString(dataModule[0]));
            cacheRadio.setSelected(true);
            dataRadio.setSelected(true);
            if(dataModule[3] == WORD_SIZE_LONG)
            {
                longWordsRadio.setSelected(true);
            }
            else
            {
                shortWordsRadio.setSelected(true);
            }
            columnSizeField.setText(Integer.toString(dataModule[1]));
            lineSizeField.setText(Integer.toString(dataModule[2]));
            createNewMemoryModule(memoryLists[1]);
        }
        for(int[] instructionModule : memories[2])
        {
            cacheField.setText(Integer.toString(instructionModule[0]));
            cacheRadio.setSelected(true);
            instructionRadio.setSelected(true);
            if(instructionModule[3] == WORD_SIZE_LONG)
            {
                longWordsRadio.setSelected(true);
            }
            else
            {
                shortWordsRadio.setSelected(true);
            }
            columnSizeField.setText(Integer.toString(instructionModule[1]));
            lineSizeField.setText(Integer.toString(instructionModule[2]));
            createNewMemoryModule(memoryLists[2]);
        }
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

        MEMORY_KIND kind;
        MEMORY_TYPE type;
        try
        {
            kind = cacheRadio.isSelected() ? MEMORY_KIND.CACHE : MEMORY_KIND.RAM;
            type = dataRadio.isSelected() ? MEMORY_TYPE.DATA : MEMORY_TYPE.INSTRUCTION;
            MemoryModule next = model.getSize() > 0 ? model.getElementAt(model.getSize() - 1) :
                                    (unifiedMemoryModel.getSize() > 0 ?
                                     unifiedMemoryModel.getElementAt(unifiedMemoryModel.getSize() - 1) :
                                         null);
            newModule = new MemoryModule(GET_ID(),
                                         kind,
                                         type,
                                         shortWordsRadio.isSelected() ? WORD_LENGTH.SHORT : WORD_LENGTH.LONG,
                                         kind.equals(MEMORY_KIND.CACHE) ? DEFAULT_CACHE_WRITE_MODE : DEFAULT_RAM_WRITE_MODE,
                                         next,
                                         Integer.parseInt(columnSizeField.getText()),
                                         Integer.parseInt(lineSizeField.getText()),
                                         cacheRadio.isSelected() ? cacheDelay : ramDelay);
            model.addElement(newModule);
            if(next == null)
            {
                newModule.storeFiles(PATH_TO_BINARIES, 0);
                registerBanks[INTERNAL_BANK_INDEX].store(CM_INDEX, newModule.getMemoryAddress());
            }
            if((instructionCachesModel.isEmpty() && kind.equals(MEMORY_KIND.RAM)) || (type.equals(MEMORY_TYPE.INSTRUCTION) && kind.equals(MEMORY_KIND.CACHE)))
            {
                pipeline.setNearestInstructionCache(newModule);
            }
            if((dataCachesModel.isEmpty() && kind.equals(MEMORY_KIND.RAM)) || (type.equals(MEMORY_TYPE.DATA) && kind.equals(MEMORY_KIND.CACHE)))
            {
                pipeline.setNearestDataCache(newModule);
            }
        }
        catch(NumberFormatException e)
        {
            WARN(logger, "Memory interface received invalid device parameters.");
        }
    }

    /**
     * @return Numerical content of address text field.
     */
    private int getAddress()
    {
                    // Must parse as long so that 32-character inputs are accepted
        return (int)Long.parseLong(addressField.getText(), getRadices()[0]);
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
            if(valueIsInternalRegister())
            {
                return Arrays.asList(INTERNAL_REGISTER_NAMES).indexOf(text);
            }
            else if(valueIsGeneralRegister())
            {
                text = text.substring(1);
            }
            else if(valueBinRadio.isSelected())
            {
                radix = 2;
            }
            else if(valueHexRadio.isSelected())
            {
                radix = 16;
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
        return new int[] { addressBinRadio.isSelected() ? 2 : (addressDecRadio.isSelected() ? 10 : 16), valueBinRadio.isSelected() ? 2 : (valueDecRadio.isSelected() ? 10 : 16) };
    }

    /**
     * Stores value in argument text field or corresponding register to given address in selected MemoryModule.
     */
    private void storeInCurrentMemoryModule()
    {
        try
        {
            if(currentlySelectedMemory == null) { throw new NumberFormatException(); }

            int[] newValueS = new int[] { getValue() };

            RegisterFileModule bank = registerBanks[INDEXABLE_BANK_INDEX];
            if(valueIsRegister())
            {
                int register = newValueS[0];
                if(newValueS[0] > 15)
                {
                    bank = registerBanks[INTERNAL_BANK_INDEX];
                    newValueS[0] -= 15;
                }
                if(lineRadio.isSelected()) { newValueS = new int[currentlySelectedMemory.getLineSize()]; }
                for(int i = 0; i < newValueS.length; i++)
                {
                    newValueS[i] = (int)bank.load(register + i);
                }
            }

            LinkedList<MemoryRequest> request = new LinkedList<>(List.of(
                                                new MemoryRequest(valueIsRegister() ? bank.getID() : -1,
                                                                  currentlySelectedMemory.getID(),
                                                                  dataRadio.isSelected() ? MEMORY_TYPE.DATA : MEMORY_TYPE.INSTRUCTION,
                                                                  REQUEST_TYPE.STORE,
                                                                  new Object[]{getAddress(), newValueS})));
            currentlySelectedMemory.store(request);
        }
        catch(NumberFormatException e)
        {
            WARN(logger, "Memory interface received invalid store parameters.");
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
            if(currentlySelectedMemory == null) { throw new NumberFormatException(); }

            int register = getValue();
            RegisterFileModule bank = registerBanks[INDEXABLE_BANK_INDEX];
            if(register > 15)
            {
                bank = registerBanks[INTERNAL_BANK_INDEX];
                register -= 15;
            }

            LinkedList<MemoryRequest> request = new LinkedList<>(List.of(
                                                new MemoryRequest(bank.getID(), currentlySelectedMemory.getID(),
                                                                  currentlySelectedMemory.getType(), REQUEST_TYPE.LOAD,
                                                                  new Object[]{getAddress(), lineRadio.isSelected()})));
            int[] line = currentlySelectedMemory.load(request);

            int wroteToIndexable = 0;
            for(int i = 0; (i < line.length) && ((register + i) < bank.getNumRegisters()); i++)
            {
                bank.store(register + i, line[i]);
                if(register <= 15)
                {
                    wroteToIndexable |= (1 << (registerBanks[INDEXABLE_BANK_INDEX].getNumRegisters() - 1)) >>> (register + i);
                }
            }
            if(wroteToIndexable != 0)
            {
                registerBanks[REVERSAL_STACK_INDEX].store(wroteToIndexable);
                for(int r = 0; r < registerBanks[INDEXABLE_BANK_INDEX].getNumRegisters(); r++)
                {
                    registerBanks[REVERSAL_STACK_INDEX].store(registerBanks[INDEXABLE_BANK_INDEX].load(r));
                }
            }

            indexableBankDisplayText.setText(bank.getDisplayText(8, getRadices()[1]));
        }
        catch(NumberFormatException e)
        {
            WARN(logger, "Memory interface received invalid load parameters.");
        }

        updateDisplay();
    }
}
