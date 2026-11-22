/**
 * TypeScript mirrors of backend/src/main/java/com/walkthrough/api/transaction/dto/*.java
 * Java BigDecimal arrives as a JSON number; LocalDateTime as "2022-04-04T00:29:00" (no zone);
 * LocalDate as "2022-04-01".
 */

export type TransactionType = 'DEBIT' | 'CREDIT';
export type ImportStatus = 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface BankTransaction {
  id: number;
  transactionId: string;
  transactedAt: string;
  details: string;
  utr: string | null;
  type: TransactionType;
  instrument: string | null;
  amount: number;
}

/** Mirrors backend TransactionFilter record - shared by table, KPIs and counterparties. */
export interface TransactionFilter {
  type: TransactionType | null;
  q: string;
  from: string | null; // yyyy-MM-dd, inclusive
  to: string | null; // yyyy-MM-dd, inclusive
  minAmount: number | null;
  maxAmount: number | null;
}

export const EMPTY_FILTER: TransactionFilter = {
  type: null,
  q: '',
  from: null,
  to: null,
  minAmount: null,
  maxAmount: null,
};

export interface PageRequest {
  page: number;
  size: number;
  sort: string; // e.g. "transactedAt,desc"
}

export interface TransactionStats {
  totalCount: number;
  debitCount: number;
  creditCount: number;
  totalDebit: number;
  totalCredit: number;
  netAmount: number;
  averageDebit: number;
  averageCredit: number;
  largestDebit: number;
  largestCredit: number;
  firstTransactionAt: string | null;
  lastTransactionAt: string | null;
}

export interface Counterparty {
  details: string; // full text, use as search query
  name: string;
  count: number;
  total: number;
}

export interface FinancialYear {
  label: string; // "FY 2022-23"
  from: string;
  to: string;
  count: number;
}

export interface InvalidRow {
  lineNumber: number;
  reason: string;
  rawRecord: string;
}

export interface ImportResult {
  importId: number;
  fileName: string;
  status: ImportStatus;
  totalRows: number;
  importedRows: number;
  duplicateRows: number;
  invalidRows: number;
  durationMs: number;
  startedAt: string;
  invalidCsvUrl: string | null;
  invalidRowsPreview: InvalidRow[];
}

export interface ImportSummary {
  importId: number;
  fileName: string;
  status: ImportStatus;
  totalRows: number;
  importedRows: number;
  duplicateRows: number;
  invalidRows: number;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
  invalidCsvUrl: string | null;
}
