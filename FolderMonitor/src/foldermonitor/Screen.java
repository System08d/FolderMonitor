/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

// Экраны окна. Перечисление (enum) нужно, чтобы не путаться в строках-названиях.
public enum Screen {
    MAIN,             // главный экран: список папок и настройки
    SELECT_FOLDER,    // экран 1: выбор папки (или нескольких) для просмотра графиков
    OPTIONS,          // экран 2: тип графика и период
    CHART             // экран 3: сам график
}