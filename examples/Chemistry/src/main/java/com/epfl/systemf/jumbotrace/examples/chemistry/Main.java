package com.epfl.systemf.jumbotrace.examples.chemistry;

import com.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry.BalancingResult;
import com.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry.EquationFormatException;
import com.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry.RawEquation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Main {

    public static void main(String[] args) {
        // "C6H12O6 + O2 => H2O + CO2"
        // "HBr + KClO3 => Br2 + H2O + KCl"
        // "C2H4 + H2 => H8 + C5 + O3"

        JLabel resultPane = new JLabel();
        setFontSize(resultPane);
        JTextField field = new JTextField();
        field.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    try {
                        String text = field.getText();
                        RawEquation eq = RawEquation.parse(text);
                        BalancingResult res = eq.balanced();
                        resultPane.setText(res.toString());
                    } catch (EquationFormatException exc){
                        resultPane.setText(exc.getMessage());
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
        setFontSize(field);

        JFrame frame = new JFrame("Enter equation and press enter");
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.add(field, BorderLayout.NORTH);
        frame.add(resultPane, BorderLayout.SOUTH);

        frame.setMinimumSize(new Dimension(450, 100));
        frame.pack();

    }

    private static void setFontSize(JComponent component) {
        component.setFont(component.getFont().deriveFont(18f));
    }

}
