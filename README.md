CSC 365 Lab 7
Team Members: Austin Ng, Shubham Sengar

Compilation instructions:
To run program, run the command:
./gradlew run -q --console=plain

Known bugs/deficiencies:
- When prompted for an int (number of children/adults, etc.), entering non-ints will cause crash
- Reservation IDs are not checked, may cause error when adding many (10000+) reservations
- Date parsing is not perfect, dates/rates may have discrepancies
