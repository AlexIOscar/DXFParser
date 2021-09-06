package DXFParserUI;

import DXFParserPkg.DXFComponentParser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DXFParserGUI extends JFrame {
    private JButton closeButton;
    private JPanel rootPanel;
    private JButton openButton;
    private JButton parseButton;
    private JTextArea outputField;
    private JButton flushButton;

    JFileChooser fileChooser = new JFileChooser();

    public DXFParserGUI() throws HeadlessException {
        setContentPane(rootPanel);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);
        setTitle("DXF Converter");
        ImageIcon ii = new ImageIcon(DXFParserGUI.class.getResource("FormLogo.png"));
        setIconImage(ii.getImage());
        List<File> fileList = new ArrayList<>();

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        openButton.addActionListener(e -> {
            fileChooser.setDialogTitle("Выбор директории");
            // Определение режима - только каталог
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(new FileNameExtensionFilter("DXF File", "DXF"));
            int result = fileChooser.showOpenDialog(DXFParserGUI.this);

            fileList.clear();

            if (result == JFileChooser.APPROVE_OPTION) {
                StringBuilder text = new StringBuilder("Выбранные файлы:\n");
                for (File file : fileChooser.getSelectedFiles()) {
                    fileList.add(file);
                    text.append(file.getName()).append("\n");
                }
                outputField.setText(text.toString());
            }
        });

        parseButton.addActionListener(e -> {
            String extension = ".ncs";
            int counter = 0;
            for (File file : fileList) {
                DXFComponentParser parser = new DXFComponentParser(file);
                File outFile = new File(file.getAbsolutePath().replace(".dxf", extension));
                try {
                    if (!outFile.createNewFile()) {
                        JOptionPane.showMessageDialog(DXFParserGUI.this,
                                "Ошибка создания файла\n" + file.getName().replace(".dxf", extension) +
                                        "\nВозможно, файл уже существует в указанной директории");
                        continue;
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                parser.pushToFile(outFile);
                if (parser.warningLog.size() > 0) {
                    StringBuilder log = new StringBuilder();
                    for (String str : parser.warningLog) {
                        log.append(str).append("\n");
                    }
                    JOptionPane.showMessageDialog(DXFParserGUI.this,
                            "Данные во входящем файле повреждены!\n" + log);
                }
                counter++;
            }
            outputField.append(String.format("\nКонвертация выполнена, записано файлов: %d",
                    counter));
        });

        flushButton.addActionListener(e -> {
            fileList.clear();
            outputField.setText("");
        });
    }
}