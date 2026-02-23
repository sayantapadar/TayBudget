package com.example.taybudget.data.handler;

import com.example.taybudget.data.firestore.GoalDAO;
import com.example.taybudget.data.firestore.MasterAggregatesDAO;
import com.example.taybudget.data.firestore.MonthlyAggregatesDAO;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.handler.interfaces.TaskCompletionCallback;
import com.example.taybudget.data.handler.model.GoalCalculation;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Goal;
import com.example.taybudget.data.model.Income;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.enums.AggregateType;
import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.enums.DataType;
import com.example.taybudget.enums.DevUsers;
import com.example.taybudget.tools.CommonUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class GoalHandler {
    private final MasterAggregatesDAO masterAggregatesDAO;
    private final MonthlyAggregatesDAO monthlyAggregatesDAO;
    private final GoalDAO goalDAO;
    private static GoalHandler instance;
    private MasterAggregate masterAggregate;
    private List<MonthlyAggregate> monthlyAggregates;
    private List<Goal> goals;

    private GoalHandler() {
        masterAggregatesDAO = new MasterAggregatesDAO();
        monthlyAggregatesDAO = new MonthlyAggregatesDAO();
        goalDAO = new GoalDAO();
    }

    public static GoalHandler getInstance() {
        if (instance == null)
            instance = new GoalHandler();
        return instance;
    }

    public void getMasterAggregate(ResultSuccessCallback<MasterAggregate> successCallback, ResultFailureCallback failureCallback) {
        masterAggregatesDAO.getMasterAggregate(true, new ResultCallback<MasterAggregate>() {
            @Override
            public void onNoResultFound() {
                // Not invoked
            }

            @Override
            public void onError(String message, Exception exception) {
                failureCallback.onError(message, exception);
            }

            @Override
            public void onResult(MasterAggregate result, String documentId) {
                masterAggregate = result;
                successCallback.onResult(result, documentId);
            }
        });
    }

    private void addMonthlyAggregate(Date d, ResultSuccessCallback<Void> successCallback, ResultFailureCallback failureCallback) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date date = calendar.getTime();
        RecurringHandler.getInstance().getNewMonthData(date, (incomes, expenses) -> {
            String name = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);
            MonthlyAggregate monthlyAggregate = new MonthlyAggregate(DevUsers.TAY.getId(), name, date, 0, 0);
            incomes.forEach(income -> CommonUtils.updateAggregate(monthlyAggregate, income, AggregateType.MONTH, DataType.INCOME, false));
            expenses.forEach(expense -> CommonUtils.updateAggregate(monthlyAggregate, expense, AggregateType.MONTH, DataType.EXPENSE, false));
            monthlyAggregates.add(monthlyAggregate);
            successCallback.onResult(null, null);
        }, failureCallback);
    }

    public void getGoals(ResultSuccessCallback<List<Goal>> successCallback, ResultFailureCallback failureCallback, boolean persist) {
        goalDAO.getAllGoals((goals, documentId) -> {
            if (persist)
                this.goals = goals;
            successCallback.onResult(goals, null);
        }, failureCallback);
    }

    public void delete(Goal goal, ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {
        goalDAO.removeGoal(goal, successCallback, failureCallback);
    }

    public void calculate(double amount, double balance, int months, Map<CategoryEnum, Double> weights, ResultSuccessCallback<GoalCalculation> successCallback, ResultFailureCallback failureCallback) {
        if (this.masterAggregate == null)
            getMasterAggregate((result, documentId) -> calculate(amount, balance, months, weights, successCallback, failureCallback), failureCallback);
        else if (goals == null)
            getGoals(((result, documentId) -> calculate(amount, balance, months, weights, successCallback, failureCallback)), failureCallback, true);
        else if (monthlyAggregates == null || monthlyAggregates.size() != months) {
            if (monthlyAggregates == null) {
                monthlyAggregates = new ArrayList<>();
                addMonthlyAggregate(Calendar.getInstance().getTime(), (result, documentId) -> calculate(amount, balance, months, weights, successCallback, failureCallback), failureCallback);
            } else {
                Date d = monthlyAggregates.get(monthlyAggregates.size() - 1).getDate();
                addMonthlyAggregate(d, (result, documentId) -> calculate(amount, balance, months, weights, successCallback, failureCallback), failureCallback);
            }
        } else {
            Gson gson = new Gson();
            MasterAggregate masterAggregate = gson.fromJson(gson.toJson(this.masterAggregate), MasterAggregate.class);
            List<MonthlyAggregate> monthlyAggregates = gson.fromJson(gson.toJson(this.monthlyAggregates), new TypeToken<List<MonthlyAggregate>>(){}.getType());

            // updating with previous goals
            goals.forEach(goal -> {
                masterAggregate.updateBalanceExpense(goal.getMasterBalanceUsed());
            });
            monthlyAggregates.forEach(monthlyAggregate -> {
                goals.forEach(goal -> {
                    for (CategoryEnum category : CategoryEnum.values()) {
                        if (goal.getSavings().containsKey(monthlyAggregate.getName()))
                            monthlyAggregate.updateCategoricalUsageLimit(category.getName(),
                                    -goal.getSavings().get(monthlyAggregate.getName()).get(category.getName()));
                    }
                });
            });
            double excess = 0;  // later usage might reduce master balance below goals usage
            if (masterAggregate.getBalance() < 0) {
                excess = -masterAggregate.getBalance();
                masterAggregate.setBalance(0);
            }

            GoalCalculation calculation = new GoalCalculation(monthlyAggregates, masterAggregate, amount, Calendar.getInstance().getTime());
            calculation.copyMonthlyAggregates();
            calculation.copyMasterAggregate();
            calculation.setWeightsType(weights);
            CommonUtils.scaleWeights(weights, 1);

            if (balance < amount) {
                boolean flag = false;
                double fromBalance = amount - balance + excess;
                calculation.getUpdatedMasterAggregate().updateBalanceExpense(balance);
                Map<String, Double> fromMonths = new HashMap<>();  // monthly total balances combining all categories
                for (MonthlyAggregate aggregate : monthlyAggregates) {
                    double monthlyBalance = 0;
                    for (CategoryEnum category : CategoryEnum.values()) {
                        monthlyBalance += aggregate.getBalanceFromCategoricalUsage(category);
                    }
                    fromMonths.put(aggregate.getName(), monthlyBalance);
                }
                double maxFromMonths = fromMonths.values().stream().reduce(Double::sum).get();

                if (maxFromMonths > fromBalance) {
                    double total = 0;
                    for (MonthlyAggregate aggregate : calculation.getUpdatedMonthlyAggregates()) {
                        double portion = fromMonths.get(aggregate.getName()) / maxFromMonths * fromBalance;
                        Map<CategoryEnum, Double> categoricalSplit = new HashMap<>();
                        for (CategoryEnum category : CategoryEnum.values()) {  // splitting monthly portion into categorical weights assigned
                            categoricalSplit.put(category, CommonUtils.roundUp(weights.get(category) * portion, 100));
                        }
                        if (Arrays.asList(CategoryEnum.values()).stream().anyMatch(category -> aggregate.getBalanceFromCategoricalUsage(category) < categoricalSplit.get(category))) {  // if the spilt is supported by balance
                            calculation.addComment("Not possible");
                            calculation.addComment("For " + aggregate.getName() + " weight distribution cannot support monthly savings portion required.");
                            CategoryEnum problemCat = Arrays.asList(CategoryEnum.values()).stream()
                                    .filter(category -> aggregate.getBalanceFromCategoricalUsage(category) < categoricalSplit.get(category))
                                    .findFirst().get();
                            calculation.addComment(problemCat.getName() + " | Balance: " + String.format(Locale.getDefault(), "%.2f", aggregate.getBalanceFromCategoricalUsage(problemCat)) + " | Required: " + String.format(Locale.getDefault(), "%.2f", categoricalSplit.get(problemCat)));
                            calculation.addComment("Decrease weight of " + problemCat.getName() + " and increase weight of rest");
                            successCallback.onResult(calculation, null);
                            calculation.setSuccess(false);
                            flag = true;
                            break;
                        }
                        // the split is possible
                        for (CategoryEnum category : CategoryEnum.values()) {  // splitting monthly portion into categorical weights assigned
                            aggregate.updateCategoricalUsageLimit(category.getName(), -categoricalSplit.get(category));
                            total += categoricalSplit.get(category);
                        }
                    }
                    if (!flag && (total + balance) >= amount) {
                        calculation.addComment("Success");
                        calculation.addComment((amount - fromBalance) + " was used from balance. " + fromBalance + " has been split up into " + months + " months");
                        calculation.addComment("Total savings will be " + (total + balance));
                        calculation.setSuccess(true);
                        calculation.setExpectedSaving((total + balance));
                    } else if (!flag && (total + balance) < amount) {
                        calculation.addComment("Not possible");
                        calculation.addComment("Maximum total after recurring costs in " + months + " months is " + (total + balance));
                        calculation.addComment("Re-adjust the weights of categories.");
                        calculation.setSuccess(false);
                    }
                } else {
                    calculation.addComment("Not possible");
                    calculation.addComment("Maximum total after recurring costs in " + months + " months is " + (maxFromMonths + balance));
                    calculation.setSuccess(false);
                }
            } else {
                calculation.getUpdatedMasterAggregate().updateBalanceExpense(amount);
                calculation.addComment("Success");
                calculation.addComment("Goal amount available in current balance");
                calculation.setMonthlyAggregates(new ArrayList<>());
                calculation.copyMonthlyAggregates();
                calculation.setSuccess(true);
                calculation.setExpectedSaving(amount);
            }
            successCallback.onResult(calculation, null);
        }
    }

    public void destroy() {
        if (monthlyAggregates != null)
            monthlyAggregates.clear();
        if (goals != null)
            goals.clear();
        masterAggregate = null;
        monthlyAggregates = null;
        goals = null;
    }

    public void saveGoal(String name, GoalCalculation calculation, TaskCompletionCallback taskCompletionCallback) {
        Goal goal = new Goal(DevUsers.TAY.getId(), name, calculation);
        goalDAO.insertGoal(goal,
                (message, documentId) -> taskCompletionCallback.taskComplete(message),
                ((message, exception) -> taskCompletionCallback.error(message)));
    }
}
