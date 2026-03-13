import { renderHook } from '@testing-library/react';
import { useEnableAddFields } from './useEnableAddFields';
import { describe, it, expect } from 'vitest';

// Mock types based on the hook usage
type SubSection = {
  endDate?: string | Date | null;
}[];

type SubSections = SubSection[];

describe('useEnableAddFields', () => {
  describe('when isDirty is false', () => {
    it('should return false', () => {
      const subSection: SubSections = [[{ endDate: '2023-12-01' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, false));
      expect(result.current).toBe(false);
    });
  });

  describe('when subSection is empty', () => {
    it('should return false', () => {
      const subSection: SubSections = [];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      expect(result.current).toBe(false);
    });
  });

  describe('when last field is empty', () => {
    it('should return false', () => {
      const subSection: SubSections = [[]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(false);
    });
  });

  describe('when endDate is missing', () => {
    it('should return true', () => {
      const subSection: SubSections = [[{}]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });

    it('should return true when endDate is undefined', () => {
      const subSection: SubSections = [[{ endDate: undefined }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });

    it('should return true when endDate is null', () => {
      const subSection: SubSections = [[{ endDate: null }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });
  });

  describe('when endDate format is invalid', () => {
    it('should return true for invalid date format', () => {
      const subSection: SubSections = [[{ endDate: 'invalid-date' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });

    it('should return true for wrong format (DD-MM-YYYY)', () => {
      const subSection: SubSections = [[{ endDate: '01-12-2023' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });

    it('should return true for incomplete date (only year)', () => {
      const subSection: SubSections = [[{ endDate: '2023' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });

    it('should return true for invalid month', () => {
      const subSection: SubSections = [[{ endDate: '2023-13-01' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });

    it('should return true for invalid day', () => {
      const subSection: SubSections = [[{ endDate: '2023-12-32' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });

    it('should return true for month 00', () => {
      const subSection: SubSections = [[{ endDate: '2023-00-01' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(true);
    });
  });


  describe('when endDate is valid', () => {
    it('should return false for valid YYYY-MM format', () => {
      const subSection: SubSections = [[{ endDate: '2023-12' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(false);
    });

    it('should return false for valid YYYY-MM-DD format', () => {
      const subSection: SubSections = [[{ endDate: '2023-12-15' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(false);
    });

    it('should return false for valid leap year date', () => {
      const subSection: SubSections = [[{ endDate: '2024-02-29' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(false);
    });

    it('should return false for end of month dates', () => {
      const subSection: SubSections = [[{ endDate: '2023-01-31' }]];
      const { result } = renderHook(() => useEnableAddFields(subSection, true));
      
      expect(result.current).toBe(false);
    });
  });

  describe('memoization', () => {
    it('should memoize the result when dependencies do not change', () => {
      const subSection: SubSections = [[{ endDate: '2023-12-15' }]];
      const { result, rerender } = renderHook(
        ({ subSection, isDirty }) => useEnableAddFields(subSection, isDirty),
        { initialProps: { subSection, isDirty: true } }
      );
      
      const firstResult = result.current;
      
      rerender({ subSection, isDirty: true });
      
      expect(result.current).toBe(firstResult);
      expect(result.current).toBe(false);
    });

    it('should recalculate when subSection changes', () => {
      const initialSubSection: SubSections = [[{ endDate: '2023-12-15' }]];
      const { result, rerender } = renderHook(
        ({ subSection, isDirty }) => useEnableAddFields(subSection, isDirty),
        { initialProps: { subSection: initialSubSection, isDirty: true } }
      );
      
      expect(result.current).toBe(false);

      const newSubSection: SubSections = [[{ endDate: 'invalid' }]];
      rerender({ subSection: newSubSection, isDirty: true });
      
      expect(result.current).toBe(true);
    });

    it('should recalculate when isDirty changes', () => {
      const subSection: SubSections = [[{ endDate: '2023-12-15' }]];
      const { result, rerender } = renderHook(
        ({ subSection, isDirty }) => useEnableAddFields(subSection, isDirty),
        { initialProps: { subSection, isDirty: false } }
      );
      expect(result.current).toBe(false);
      rerender({ subSection, isDirty: true });
      expect(result.current).toBe(false);
    });
  });
});
