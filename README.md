CSC 365 Lab 7
Team Members: Austin Ng, Shubham Sengar

Compilation instructions:
To run program, run the command:
./gradlew run -q --console=plain

Known bugs/deficiencies:
- Input prompts are very strict, program will crash if incorrect types are given (date, int, etc)
- Reservation IDs are not checked, may cause error when adding many (10000+) reservations
- Date parsing is not perfect, dates/rates may have discrepancies
