# Pending
## Checkbox for credit card
- If true
  - Don't add to balances now
  - Only add to daily budget
- Else
  - Current behaviour
- How?
  - add attribute - credit
  - If credit is true, don't update bank balance in aggregates
    - Add amount to new attribute in aggregates - 'credit'
  - Idea is - later when credit card payment is done, Credit Card Payment type expense will be added
    - Update the bank balance when this is added
- Add another expense type - Credit Card Payment
  - Subtract amount from 'credit' attribute in aggregates
  - Update aggregates' bank balances
## Add new auto identification regex in DB
- store the previous one in local storage
- check for update against the new one
- if changed, 
  - read all messages from past 3 months 
  - filter with the update *only*
  - add to list 
  - sort