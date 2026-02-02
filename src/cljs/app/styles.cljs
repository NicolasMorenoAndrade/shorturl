(ns app.styles)

;; Define styles map outside component for reusability
(def styles
  {:container "bg-purple-100 grid place-items-center h-screen p-2"
   :card "bg-white rounded-lg shadow-md p-8 w-full max-w-md"
   :title "text-2xl font-bold text-purple-800 mb-6 text-center"
   :auth
   {:container "absolute top-2 right-2"
    :button "text-sm text-gray-500 hover:text-purple-600 bg-transparent p-1 rounded-full transition-colors duration-200"
    :user-icon-clicked "h-6 w-6 rounded-full bg-purple-500 flex items-center justify-center text-purple-700 text-xs font-bold"
    :user-icon-unclicked "h-6 w-6 rounded-full bg-purple-100 cursor-pointer flex items-center justify-center text-purple-700 text-xs font-bold"
    :dropdown "absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 z-10 border border-gray-100"
    :dropdown-item "block px-4 py-2 text-sm text-gray-700 hover:text-purple-600 w-full text-left"
    :user-info "px-4 py-2 border-b border-gray-100"
    :welcome "text-sm font-medium text-gray-700"
    :email "text-xs text-gray-500"}

   :slugs-section
   {:container "mt-6 border-t border-gray-200 pt-4"
    :header "flex justify-between items-center cursor-pointer hover:bg-gray-50 p-2 rounded transition-colors"
    :title "text-lg font-semibold text-gray-700"
    :toggle "text-gray-500 text-xl"
    :list "mt-4 space-y-3"
    :item {:container "border border-gray-200 rounded p-3 hover:border-purple-300 transition-colors"
           :slug "text-purple-600 hover:text-purple-700 cursor-pointer font-medium text-lg"
           :url "text-sm text-gray-500 mt-1 break-all"
           :delete-btn "text-red-500 hover:text-red-700 text-sm px-2 py-1 rounded hover:bg-red-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"}
    :empty "text-center text-gray-500 py-8 italic"
    :loading "text-center text-gray-500 py-4"}

   :result-section
   {:container "text-center"
    :label "mb-3 text-gray-600"
    :link "text-purple-500 hover:text-purple-600 font-medium text-lg break-all"
    :button "mt-6 w-full bg-purple-600 hover:bg-purple-700 text-white py-2 px-4 rounded transition-colors duration-200 cursor-pointer"}

   :form
   {:container "space-y-4"
    :label "block text-sm font-medium text-gray-700 mb-1"
    :input "w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
    :button {:base "w-full bg-purple-600 text-white py-2 px-4 rounded-md transition-colors duration-200 font-medium mt-2"
             :enabled "hover:bg-purple-700 cursor-pointer"
             :disabled "opacity-70 cursor-not-allowed"}}
   :loading
   {:container "flex items-center justify-center"
    :spinner "animate-spin mr-2 h-4 w-4 border-t-2 border-b-2 border-white rounded-full"}
   :error "text-red-500 text-sm"})
