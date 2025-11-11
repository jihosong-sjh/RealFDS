import { useMemo } from 'react';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  flexRender,
  type ColumnDef,
  type SortingState,
  type OnChangeFn,
} from '@tanstack/react-table';
import type { Alert } from '../types/alert';
import '../styles/AlertHistoryTable.css';

/**
 * AlertHistoryTable Props 정의
 */
interface AlertHistoryTableProps {
  /** 표시할 알림 목록 */
  data: Alert[];
  /** 정렬 상태 */
  sorting?: SortingState;
  /** 정렬 변경 핸들러 */
  onSortingChange?: OnChangeFn<SortingState>;
}

/**
 * AlertHistoryTable 컴포넌트
 *
 * TanStack Table (React Table v8)을 사용하여 알림 목록을 표시합니다.
 * - 알림 ID, 사용자 ID, 금액, 규칙명, 심각도, 발생 시각, 상태 컬럼 표시
 * - 정렬 기능 지원 (alertTimestamp 기본 내림차순)
 * - 한국어 컬럼 헤더
 * - TypeScript 타입 안정성
 */
export function AlertHistoryTable({
  data,
  sorting = [{ id: 'alertTimestamp', desc: true }],
  onSortingChange,
}: AlertHistoryTableProps) {
  /**
   * 테이블 컬럼 정의
   */
  const columns = useMemo<ColumnDef<Alert>[]>(
    () => [
      {
        id: 'alertId',
        accessorKey: 'alertId',
        header: '알림 ID',
        cell: (info) => {
          const alertId = info.getValue() as string;
          return (
            <span className="alert-history-table__cell--id" title={alertId}>
              {alertId.substring(0, 8)}...
            </span>
          );
        },
        enableSorting: false,
      },
      {
        id: 'userId',
        accessorFn: (row) => row.originalTransaction?.userId,
        header: '사용자 ID',
        cell: (info) => info.getValue() || 'N/A',
        enableSorting: true,
      },
      {
        id: 'amount',
        accessorFn: (row) => row.originalTransaction?.amount,
        header: '금액',
        cell: (info) => {
          const amount = info.getValue() as number;
          const currency = info.row.original.originalTransaction?.currency || '';
          return (
            <span className="alert-history-table__cell--amount">
              {amount.toLocaleString()} {currency}
            </span>
          );
        },
        enableSorting: true,
      },
      {
        id: 'ruleName',
        accessorKey: 'ruleName',
        header: '규칙명',
        cell: (info) => info.getValue(),
        enableSorting: true,
      },
      {
        id: 'severity',
        accessorKey: 'severity',
        header: '심각도',
        cell: (info) => {
          const severity = info.getValue() as string;
          return (
            <span
              className={`alert-history-table__severity alert-history-table__severity--${severity.toLowerCase()}`}
            >
              {severity}
            </span>
          );
        },
        enableSorting: true,
      },
      {
        id: 'alertTimestamp',
        accessorKey: 'alertTimestamp',
        header: '발생 시각',
        cell: (info) => {
          const timestamp = info.getValue() as string;
          return (
            <span className="alert-history-table__cell--timestamp">
              {new Date(timestamp).toLocaleString('ko-KR')}
            </span>
          );
        },
        enableSorting: true,
      },
      {
        id: 'status',
        accessorKey: 'status',
        header: '상태',
        cell: (info) => {
          const status = info.getValue() as string;
          return (
            <span
              className={`alert-history-table__status alert-history-table__status--${status.toLowerCase()}`}
            >
              {status}
            </span>
          );
        },
        enableSorting: true,
      },
    ],
    []
  );

  /**
   * TanStack Table 인스턴스 생성
   */
  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
    },
    onSortingChange,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  return (
    <div className="alert-history-table">
      <table className="alert-history-table__table">
        <thead className="alert-history-table__thead">
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id} className="alert-history-table__header-row">
              {headerGroup.headers.map((header) => (
                <th
                  key={header.id}
                  className="alert-history-table__header-cell"
                  onClick={header.column.getToggleSortingHandler()}
                  style={{
                    cursor: header.column.getCanSort() ? 'pointer' : 'default',
                  }}
                >
                  <div className="alert-history-table__header-content">
                    {flexRender(header.column.columnDef.header, header.getContext())}
                    {/* 정렬 인디케이터 */}
                    {header.column.getCanSort() && (
                      <span className="alert-history-table__sort-indicator">
                        {{
                          asc: ' ↑',
                          desc: ' ↓',
                        }[header.column.getIsSorted() as string] ?? ' ⇅'}
                      </span>
                    )}
                  </div>
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody className="alert-history-table__tbody">
          {table.getRowModel().rows.map((row) => (
            <tr key={row.id} className="alert-history-table__row">
              {row.getVisibleCells().map((cell) => (
                <td key={cell.id} className="alert-history-table__cell">
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>

      {/* 빈 결과 메시지 */}
      {data.length === 0 && (
        <div className="alert-history-table__empty">
          <p>검색 조건에 맞는 알림이 없습니다.</p>
        </div>
      )}
    </div>
  );
}
