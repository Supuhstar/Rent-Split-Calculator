/*
 *  Rent-Split.kt
 *  Made for Rent Split 2 by Ben Leggiero, starting 2017-11-23
 *
 *  Written in Kotlin/JS 1.2
 *
 *  Copyright Blue Husky Studios 2017 BH-1-PS
 */

@file:Suppress("MemberVisibilityCanPrivate", "LocalVariableName", "NOTHING_TO_INLINE")

package RentSplit

import jQueryInterface.*
import org.bh.tools.base.func.observing
import org.w3c.dom.Element
import org.w3c.dom.events.Event


///// APP-GLOBAL CONSTANTS /////

/// Selectors ///

val addARoommateRowId = "Add-Roommate-Row"
val addARoommateRowSelector = "#$addARoommateRowId"
val addARoommateButtonId = "Add-Roommate-Button"
val addARoommateButtonSelector = "#$addARoommateButtonId"
val removeARoommateButtonClassName = "remove-roommate-button"
val removeARoommateButtonSelector = ".$removeARoommateButtonClassName"
val addAnExpenseRowId = "Add-Expense-Row"
val addAnExpenseRowSelector = "#$addAnExpenseRowId"
val addAnExpenseButtonId = "Add-Expense-Button"
val addAnExpenseButtonSelector = "#$addAnExpenseButtonId"
val removeAnExpenseButtonClassName = "remove-expense-button"
val removeAnExpenseButtonSelector = ".$removeAnExpenseButtonClassName"

val roommateRowDataName = "roommate-row"
val roommateRowSelector = "[data-$roommateRowDataName]"
val expenseRowDataName = "expense-row"
val expenseRowSelector = "[data-$expenseRowDataName]"
val roommateResultRowDataName = "result-$roommateRowDataName"

//val roommateTableId = "Roommate"
//val roommateTableSelector = "#$roommateTableId"
val roommateNameInputClassName = "roommate-name"
val roommateNameInputSelector = "." + roommateNameInputClassName
val roommateIncomeInputClassName = "roommate-income"
val roommateIncomeInputSelector = "." + roommateIncomeInputClassName
val roommateProportionClassName = "roommate-proportion"
val roommateProportionSelector = "." + roommateProportionClassName
val roommateAnyInputFieldSelector = "$roommateNameInputSelector,$roommateIncomeInputSelector"
//val roommateResultRowSelector = "[data-$roommateResultRowDataName]"
val roommateWhoOwesTooMuchClassName = "roommate-owes-too-much"
//val roommateWhoOwesTooMuchSelector = ".$roommateWhoOwesTooMuchClassName"
val roommateRemovabilityDataName = "roommate-removable"
val roommateRemovabilityAttribute = "data-$roommateRemovabilityDataName"
val roommateRemovabilitySelector = "[$roommateRemovabilityAttribute]"
val roommateRenamabilityDataName = "roommate-renamable"
val roommateRenamabilityAttribute = "data-$roommateRenamabilityDataName"
val roommateRenamabilitySelector = "[$roommateRenamabilityAttribute]"

val expenseTypeInputClassName = "expense-type"
val expenseTypeInputSelector = "." + expenseTypeInputClassName
val expenseCostInputClassName = "expense-cost"
val expenseCostInputSelector = "." + expenseCostInputClassName
val expenseAnyInputFieldSelector = "$expenseTypeInputSelector,$expenseCostInputSelector"
//val expenseTableSelector = "#Expenses"
//val expenseTableBodySelector = expenseTableSelector + ">tbody"
val expenseRemovabilityDataName = "expense-removable"
val expenseRemovabilityAttribute = "data-$expenseRemovabilityDataName"
val expenseRemovabilitySelector = "[$expenseRemovabilityAttribute]"
val expenseRenamabilityDataName = "expense-renamable"
val expenseRenamabilityAttribute = "data-$expenseRenamabilityDataName"
val expenseRenamabilitySelector = "[$expenseRenamabilityAttribute]"

val anyInputFieldSelector = "$roommateAnyInputFieldSelector,$expenseAnyInputFieldSelector"
val anyInputButtonSelector = "$addARoommateButtonSelector,$addAnExpenseButtonSelector,$removeAnExpenseButtonSelector"
val anyInputSelector = "$anyInputFieldSelector,$anyInputButtonSelector"

//val moneyAmountInputSelector = "$roommateIncomeInputSelector,$expenseCostInputSelector"

val resultsTableSelector = "#Results"
val resultsTableBodySelector = "$resultsTableSelector>tbody"
val resultsTableHeadRowSelector = "$resultsTableSelector>thead>tr"


/// Label text ///

val rentExpenseTitle = "Rent"
val utilitiesExpenseTitle = "Utilities"

val roommateNamePlaceholderText = "Name"
val roommateIncomePlaceholderText = "Income"

val expenseTypePlaceholderText = "Type"
val expenseCostPlaceholderText = "Monthly Cost"

val roommateNameColumnTitle = "Name"
val totalColumnTitle = "Total Cost"


/// Defaults ///

val defaultRoommateIncome: Double = 1000.0

val defaultExpenseCost: Double = 100.0
val defaultRentExpenseCost: Double = 800.0
val defaultUtilitiesExpenseCost: Double = 50.0


/**
 * @author Ben Leggiero
 * @since 2017-11-23
 */
class RentSplit {

    ///// SETUP /////


    var state: RentSplitState by observing(RentSplitState.load(), didSet = { _, _ ->
        this.regenerateInputTables()
        this.reRegisterListeners()
        this.recalculateRentSplit()
    })


    fun onReady() {
        this.regenerateInputTables()
        this.registerListeners()
        this.recalculateRentSplit()
        this.presentToUser()
    }


    /**
     * De- and re-registers every listener
     */
    fun reRegisterListeners() {
        jq(anyInputSelector).off()
        this.registerListeners()
    }


    /**
     * Registers every listener
     */
    fun registerListeners() {
        jq(anyInputFieldSelector).change(::anyInputFieldDidChange)
        jq(addAnExpenseButtonSelector).click(::didPressNewExpenseButton)
        jq(removeAnExpenseButtonSelector).click(::didPressRemoveExpenseButton)
        jq(addARoommateButtonSelector).click(::didPressNewRoommateButton)
        jq(removeARoommateButtonSelector).click(::didPressRemoveRoommateButton)
    }


    fun anyInputFieldDidChange(event: Event?) {
        reloadStateFromPage()
        // implicit recalculateRentSplit()
    }


    /**
     * Re-loads the app state based on user input on the page
     */
    fun reloadStateFromPage() {
        state = RentSplitState(roommates = fetchRoommates(), expenses = fetchExpenses())
    }


    /**
     * Throws out the old calculations and recalculates every roommate's share of every expense, and displays
     * the output
     */
    fun recalculateRentSplit() {
        val roommates = state.roommates
        val expenses = state.expenses

        this.recalculateRoommateProportions(roommates)
        //val totalExpenses = this.totalExpenses(expenses)

        this.fillOutResults(roommates, expenses)

        this.notifyOfProblems(roommates, expenses)
    }


    fun presentToUser() {
        jq(".rent").addClass("rent-ready")
    }


    ///// FETCHING /////

    /**
     * Finds all roommates in the DOM, parses them into RentRoommate objects, and returns them.
     */
    fun fetchRoommates(): RentRoommates {
        return this.roommateRowsToRoommates(jq(roommateRowSelector))
    }


    /**
     * Finds all expenses in the DOM, parses them into RentExpense objects, and returns them.
     */
    fun fetchExpenses(): RentExpenses {
        return this.expenseRowsToExpenses(jq(expenseRowSelector))
    }


    /**
     * Takes in a jQuery result containing roommate input rows, parses each to a RentRoommate, and returns the
     * results in an array
     */
    fun roommateRowsToRoommates(jq_roommateRows: JQuery): RentRoommates {
        return RentRoommates(jq_roommateRows.map { index, roommateRow -> this.roommateRowToRoommate(index, jq(roommateRow)) }.asList())
    }


    /**
     * Takes in a jQuery result containing expense input rows, parses each to a RentExpense, and returns the
     * results in an array
     */
    fun expenseRowsToExpenses(jq_expenseRows: JQuery): RentExpenses {
        return RentExpenses(jq_expenseRows.map { index, expenseRow -> this.expenseRowToExpense(index, jq(expenseRow)) }.asList())
    }


    /**
     * Takes in a jQuery result containing a single roommate input row, parses it to a RentRoommate, and returns that
     */
    @Suppress("UNUSED_PARAMETER")
    fun roommateRowToRoommate(index: Int, jq_roommateRow: JQuery): RentRoommate {
        return RentRoommate(
                name = jq(roommateNameInputSelector, jq_roommateRow).`val`() ?: "",
                monthlyIncome = jq(roommateIncomeInputSelector, jq_roommateRow).`val`()?.toDoubleOrNull() ?: Double.NaN,
                isRemovable = jq_roommateRow.attr(roommateRemovabilityAttribute)?.toBooleanOrNull() ?: true,
                isRenamable = jq_roommateRow.attr(roommateRenamabilityAttribute)?.toBooleanOrNull() ?: true,
                originalDOMElement = jq_roommateRow
        )
    }


    /**
     * Takes in a jQuery result containing a single expense input row, parses it to a RentExpense, and returns that
     */
    @Suppress("UNUSED_PARAMETER")
    fun expenseRowToExpense(index: Int, jq_expenseRow: JQuery): RentExpense {
        return RentExpense(
                type = jq(expenseTypeInputSelector, jq_expenseRow).`val`() ?: "",
                monthlyCost = jq(expenseCostInputSelector, jq_expenseRow).`val`()?.toDoubleOrNull() ?: Double.NaN,
                isRemovable = jq_expenseRow.attr(expenseRemovabilityAttribute)?.toBooleanOrNull() ?: true,
                isRenamable = jq_expenseRow.attr(expenseRenamabilityAttribute)?.toBooleanOrNull() ?: true,
                originalDOMElement = jq_expenseRow
        )
    }


    ///// CALCULATION /////

    /**
     * Trows away and recalculates the total income and each roommate's proportion of that, then displays the
     * proportions in the roommate input table
     */
    fun recalculateRoommateProportions(roommates: RentRoommates) {
        val totalIncome = roommates.totalIncome
        roommates.allRoommates.forEach { this.recalculateRoommateProportion(it, totalIncome) }
        this.displayRoommateProportions(roommates)
    }


    /**
     * Throws away and recalculates each roommate's proportion of the total income
     */
    fun recalculateRoommateProportion(roommate: RentRoommate, totalIncome: Double) {
        roommate.proportion = roommate.monthlyIncome / totalIncome
    }


    /**
     * Displays each roommate's proportions of the total income in their input row
     */
    fun displayRoommateProportions(roommates: RentRoommates) {
        roommates.allRoommates.forEach(this::displayRoommateProportion)
    }


    /**
     * Displays a single roommate's proportions of the total income in their input row
     */
    fun displayRoommateProportion(roommate: RentRoommate) {
        roommate.originalDOMElement?.let { originalDOMElement ->
            jq(roommateProportionSelector, originalDOMElement).html("${((roommate.proportion ?: 0.0) * 100).toFixed(2)}%")
        }
    }


    ///// INPUT TABLES /////

    fun regenerateInputTables() {
        this.regenerateRoommateInputTable()
        this.regenerateExpenseInputTable()
    }


    private fun <Resource> regenerateInputTable(rowSelector: String,
                                                allResources: List<Resource>,
                                                configureExistingInput: (existingInput: Element, resource: Resource) -> Unit,
                                                insertNewInput: (index: Int?, newResource: Resource) -> Unit) {
        val existingInputs = jq(rowSelector).asList()
        val offset = existingInputs.size
        val newResources = allResources.subList(fromIndex = offset, toIndex = allResources.size)

        existingInputs.zip(allResources).forEach { (existingInput, expense) ->
            configureExistingInput(existingInput, expense)
        }

        newResources.forEachIndexed { index, newExpense ->
            insertNewInput(index + offset, newExpense)
        }
    }


    private fun regenerateExpenseInputTable() {
        regenerateInputTable(expenseRowSelector, state.expenses.allExpenses,
                             { existingExpenseInput, expense ->
                                 configureExistingExpenseInputRow(existingExpenseInput,
                                                                  expense)
                             }) { explicitIndex, expense -> insertNewExpenseInputRow(explicitIndex, expense) }
    }


    private fun configureExistingExpenseInputRow(existingExpenseInput: Element, expense: RentExpense) {
        val jq_existingExpenseInput = jq(existingExpenseInput)

        expense.originalDOMElement = jq_existingExpenseInput
        jq(expenseTypeInputSelector, existingExpenseInput).`val`(expense.type)
        jq(expenseCostInputSelector, existingExpenseInput).`val`(expense.monthlyCost)

        jq_existingExpenseInput.attr(expenseRenamabilityAttribute, expense.isRenamable)
        jq_existingExpenseInput.attr(expenseRemovabilityAttribute, expense.isRemovable)
    }


    private fun regenerateRoommateInputTable() {
        regenerateInputTable(roommateRowSelector, state.roommates.allRoommates, ::configureExistingRoommateInputRow, ::insertNewRoommateInputRow)
    }


    private fun configureExistingRoommateInputRow(existingRoommateInputTableRow: Element, roommate: RentRoommate) {
        val jq_existingRoommateInputTableRow = jq(existingRoommateInputTableRow)

        roommate.originalDOMElement = jq_existingRoommateInputTableRow
        jq(roommateNameInputSelector, existingRoommateInputTableRow).`val`(roommate.name)
        jq(roommateIncomeInputSelector, existingRoommateInputTableRow).`val`(roommate.monthlyIncome)

        this.displayRoommateProportion(roommate)

        jq_existingRoommateInputTableRow.attr(roommateRenamabilityAttribute, roommate.isRenamable)
        jq_existingRoommateInputTableRow.attr(roommateRemovabilityAttribute, roommate.isRemovable)
    }


    ///// ADDING ROWS /////

    /**
     * Handles the user's click of the "Add an expense" button
     */
    fun didPressNewExpenseButton(event: Event) {
        this.addNewExpense(event = event,
                           newExpense = RentExpense("",
                                                    monthlyCost = defaultExpenseCost,
                                                    isRemovable = true,
                                                    isRenamable = true))
    }


    /**
     * Adds a new expense to the calculator
     */
    @Suppress("UNUSED_PARAMETER")
    fun addNewExpense(newExpense: RentExpense, event: Event? = null) {
        state = state.addingNewExpense(newExpense)
    }


    fun insertNewExpenseInputRow(explicitIndex: Int? = null, expense: RentExpense) {
        val expenseInputHtml = buildExpenseInputRow(index = explicitIndex ?: numberOfExpensesOnPage(),
                                                    expense = expense)
        expense.originalDOMElement = jq(addAnExpenseRowSelector).before(expenseInputHtml).prev()
    }


    fun buildExpenseInputRow(index: Int, expense: RentExpense): String {
        return buildExpenseInputRow(index = index,
                                    type = expense.type,
                                    cost = expense.monthlyCost,
                                    isRenamable = expense.isRenamable,
                                    isRemovable = expense.isRemovable)
    }


    /**
     * Builds a string representation of a table row representing an expense input. If the type and cost are given,
     * they are pre-filled.
     *
     * @param index       The position of the row in the list of roommates
     * @param type        The type of expense; its name
     * @param cost        The monthly cost of the expense
     * @param isRenamable Indicates whether the type should be editable
     * @param isRemovable Indicates whether the row should be removable
     */
    fun buildExpenseInputRow(index: Int, type: String?, cost: Double?, isRenamable: Boolean, isRemovable: Boolean): String {

        var row = "<tr" +
                " data-$expenseRowDataName=\"$index\"" +
                " $expenseRenamabilityAttribute='$isRenamable'" +
                " $expenseRemovabilityAttribute='$isRemovable'" +
                ">"
        row +=
                "<th${(if (isRenamable) " class=\"plain\"" else "")}>" +
                        "<input" +
                        " type=\"${if (isRenamable) "text" else "hidden"}\"" +
                        " class=\"$expenseTypeInputClassName   text-right\"" +
                        (if (type.isNeitherNullNorEmpty()) " value=\"$type\"" else "") +
                        " size=\"8\"" +
                        " tabindex=0" +
                        " placeholder=\"$expenseTypePlaceholderText\"" +
                        "/>" +
                        (if (!isRenamable && type.isNeitherNullNorEmpty()) type else "") +
                        "</th>"

        row +=
                "<td class=\"plain vert-bottom\">" +
                        "<input" +
                        " type=\"number\"" +
                        (if (type.isNeitherNullNorEmpty()) " id=\"total-$type\"" else "") +
                        " class=\"$expenseCostInputClassName\"" +
                        " required" +
                        " value=\"${cost ?: defaultExpenseCost}\"" +
                        " step=\"10\"" +
                        " size=\"8\"" +
                        " tabindex=0" +
                        " placeholder=\"$expenseCostPlaceholderText\"" +
                        "/>" +
                        "</td>"

        if (isRemovable) {
            row +=
                    "<td" +
                            " class=\"$removeAnExpenseButtonClassName color-danger\"" +
                            " tabindex=\"0\">" +
                            "<i class=\"fa fa-minus-circle\"></i>" +
                            "</td>"
        }
        return row + "</tr>"
    }


    /**
     * Handles the user's click of the "Add a roommate" button
     */
    fun didPressNewRoommateButton(event: Event) {
        return this.addNewRoommate(event = event,
                                   newRoommate = RentRoommate(name = "",
                                                              monthlyIncome = defaultRoommateIncome,
                                                              isRemovable = true,
                                                              isRenamable = true))
    }


    /**
     * Adds a new roommate to the calculator
     */
    @Suppress("UNUSED_PARAMETER")
    fun addNewRoommate(newRoommate: RentRoommate, event: Event? = null) {
        state = state.addingNewRoommate(newRoommate)
    }


    fun insertNewRoommateInputRow(explicitIndex: Int? = null, roommate: RentRoommate) {
        val roommateInputHtml = buildRoommateInputRow(index = explicitIndex ?: numberOfExpensesOnPage(),
                                                    roommate = roommate)
        roommate.originalDOMElement = jq(addARoommateRowSelector).before(roommateInputHtml).prev()
    }


    fun buildRoommateInputRow(index: Int, roommate: RentRoommate): String {
        return buildRoommateInputRow(index = index,
                                    name = roommate.name,
                                    income = roommate.monthlyIncome,
                                    isRenamable = roommate.isRenamable,
                                    isRemovable = roommate.isRemovable)
    }


    /**
     * Builds a string representation of a table row representing an roommate input. If the name and income are
     * given, they are pre-filled.
     *
     * @param index       The position of the row in the list of roommates
     * @param name        The name of the roommate
     * @param income      The monthly income of the roommate
     * @param isRenamable Indicates whether the row should be renamable (currently unused)
     * @param isRemovable Indicates whether the row should be removable
     */
    fun buildRoommateInputRow(index: Int, name: String?, income: Double?, isRenamable: Boolean, isRemovable: Boolean): String  {

        val roommateNumber = numberOfRoommates() + 1

        var row = "<tr" +
                " data-$roommateRowDataName='$roommateNumber'" +
                " $roommateRenamabilityAttribute='$isRenamable'" +
                " $roommateRemovabilityAttribute='$isRemovable'" +
                ">"
        row +=
                "<th class=\"plain\">" +
                        "<input" +
                        " type=\"text\"" +
                        " class=\"$roommateNameInputClassName   text-right\"" +
                        (if (name.isNeitherNullNorEmpty()) " value=\"" + name + "\"" else "") +
                        " size=\"8\"" +
                        " tabindex=0" +
                        " placeholder=\"$roommateNamePlaceholderText\"" +
                        "/>" +
                        "</th>"

        row +=
                "<td class=\"plain vert-bottom\">" +
                        "<input" +
                        " type=\"number\"" +
                        " class=\"$roommateIncomeInputClassName\"" +
                        " required" +
                        " value=\"${income ?: defaultRoommateIncome}\"" +
                        " step=\"100\"" +
                        " size=\"8\"" +
                        " tabindex=0" +
                        " placeholder=\"$roommateIncomePlaceholderText\"" +
                        "/>" +
                        "</td>"

        row += "<td class=\"$roommateProportionClassName\">Calculating</td>"

        if (isRemovable) {
            row +=
                    "<td class=\"$removeARoommateButtonClassName color-danger\"" +
                            " tabindex=\"0\">" +
                            "<i class=\"fa fa-minus-circle\"></i>" +
                            "</td>"
        }
        return row + "</tr>"
    }


    fun numberOfRoommates(): Int {
        return state.roommates.allRoommates.size
    }


    fun numberOfRoommatesOnPage(): Int {
        return jq(roommateRowSelector).length
    }


    fun numberOfExpenses(): Int {
        return state.expenses.allExpenses.size
    }


    fun numberOfExpensesOnPage(): Int {
        return jq(expenseRowSelector).length
    }


    ///// REMOVING ROWS /////

    /**
     * Removes the expense input row referenced in the given event
     */
    fun didPressRemoveExpenseButton(event: Event) {
        val expenseRow = event.currentTarget?.parentElement
        expenseRow?.remove()
        reloadStateFromPage()
    }


    /**
     * Removes the roommate input row referenced in the given event
     */
    fun didPressRemoveRoommateButton(event: Event) {
        val roommateRow = event.currentTarget?.parentElement
        roommateRow?.remove()
        reloadStateFromPage()
    }


    ///// OUTPUT /////

    /**
     * Using the given roommates and expenses, this throws away and regenerates the Results output table
     */
    fun fillOutResults(roommates: RentRoommates, expenses: RentExpenses) {
        this.fillOutResultsTableHead(roommates, expenses)
        this.fillOutResultsTableBody(roommates, expenses)
    }


    /**
     * Using the given roommates and expenses, generates and outputs the table column heads to the Results
     * output table
     */
    @Suppress("UNUSED_PARAMETER")
    fun fillOutResultsTableHead(roommates: RentRoommates, expenses: RentExpenses) {
        val jq_resultsTableHeadRow = jq(resultsTableHeadRowSelector)
        jq_resultsTableHeadRow.empty()
        jq_resultsTableHeadRow.append("<th class=\"text-center\">$roommateNameColumnTitle</th>")
        expenses.allExpenses.forEach { this.appendExpenseColumn(jq_resultsTableHeadRow, it) }
        jq_resultsTableHeadRow.append("<th class=\"text-center\">$totalColumnTitle</th>")
    }


    /**
     * Using the given expense, generates and outputs the table column head to the Results output table
     */
    fun appendExpenseColumn(jq_resultsTableHeadRow: JQuery, expense: RentExpense) {
        jq_resultsTableHeadRow.append("<th class='hide-small'>${expense.type}</th>")
    }


    /**
     * Using the given roommates and expenses, generates and outputs the roommate table rows to the Results
     * output table
     */
    fun fillOutResultsTableBody(roommates: RentRoommates, expenses: RentExpenses) {
        val jq_resultsTableBody = jq(resultsTableBodySelector)
        jq_resultsTableBody.empty()
        roommates.allRoommates.forEach { this.appendResultRow(jq_resultsTableBody, it, expenses) }
    }


    /**
     * Using the given roommate and expenses, generates and outputs the table row to the Results output table
     */
    fun appendResultRow(jq_resultsTableBody: JQuery, roommate: RentRoommate, expenses: RentExpenses) {
        jq_resultsTableBody.append(this.buildResultRow(rowIndex = numberOfRoommates(),
                                                       roommate = roommate,
                                                       expenses = expenses))
    }


    /**
     * Builds a string representation of a Results table row.
     */
    fun buildResultRow(rowIndex: Int, roommate: RentRoommate, expenses: RentExpenses): String {
        val roommateName = RentRoommate.name(ideal = roommate.name.nonEmptyOrNull(), backupNumber = rowIndex + 1)
        var row = "<tr data-$roommateResultRowDataName='$roommateName'><th>$roommateName</th>"
        row += expenses.allExpenses.joinToString(
                separator = "",
                transform = { "<td class='hide-small'>${roommateContribution(roommate, it).dollarFormat}</td>" })
        row += "<th>${roommateTotalContributions(roommate, totalExpenses = expenses.totalExpenses).dollarFormat}</th>"
        return "$row</tr>"
    }


    fun roommateContribution(roommate: RentRoommate, expense: RentExpense): Double
            = (roommate.proportion ?: 0.0) * expense.monthlyCost


    /**
     * Finds the total amount that the given roommate will contribute
     */
    fun roommateTotalContributions(roommate: RentRoommate, totalExpenses: Double): Double
            = (roommate.proportion ?: 0.0) * totalExpenses


    ///// CHECKING /////

    fun notifyOfProblems(roommates: RentRoommates, expenses: RentExpenses) {
        val roommatesWhoOweTooMuch = roommates.allRoommates.mapIndexed { index, roommate ->  Pair(index, roommate) }.filter { (_, roommate) ->
            val roommateTotalContributions = roommateTotalContributions(roommate, expenses.totalExpenses)
            return@filter roommateTotalContributions > roommate.monthlyIncome
        }

        roommatesWhoOweTooMuch.forEach { (index, roommate) ->
            jq("[data-$roommateResultRowDataName='${roommate.nonEmptyName(index = index) }']")
                    .addClass(roommateWhoOwesTooMuchClassName)
                    .attr("title", "This roommate owes too much!")
        }
    }
}



fun main(args: Array<String>) {
    jq({
           RentSplit().onReady()
       })
}
