export type CategoryType = "INCOME" | "EXPENSE";

export type Category = {
  id: string;
  name: string;
  type: CategoryType;
  parentId: string | null;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
};
